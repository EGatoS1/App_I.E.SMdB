package com.example.app_iesmdb

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TeacherStudentsFragment : Fragment(R.layout.fragment_teacher_students) {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var adapter: TeacherStudentsAdapter
    private lateinit var rv: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var tvP: TextView
    private lateinit var tvT: TextView
    private lateinit var tvF: TextView

    // selector de fecha
    private lateinit var tvDate: TextView
    private lateinit var btnPrev: TextView
    private lateinit var btnNext: TextView
    private val cal = Calendar.getInstance()

    // Lista completa (para filtros)
    private var fullList: List<TeacherStudentUI> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv = view.findViewById(R.id.rvStudents)
        etSearch = view.findViewById(R.id.etSearchCode)
        tvP = view.findViewById(R.id.tvCountPresent)
        tvT = view.findViewById(R.id.tvCountLate)
        tvF = view.findViewById(R.id.tvCountAbsent)

        tvDate = view.findViewById(R.id.tvSelectedDate)
        btnPrev = view.findViewById(R.id.btnPrevDay)
        btnNext = view.findViewById(R.id.btnNextDay)

        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = TeacherStudentsAdapter()
        rv.adapter = adapter

        // 1) Grado del tutor (SavedStateHandle o Intent)
        val grade: String? = findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.get<String>("GRADE")
            ?: requireActivity().intent.getStringExtra("GRADE")

        if (grade.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Sin grado del tutor", Toast.LENGTH_SHORT).show()
            return
        }

        // 2) Modo demo (mock)
        val useMock = resources.getBoolean(R.bool.use_mock_students)
        if (useMock) {
            updateDateLabel()
            fullList = mockStudentsData()
            adapter.submitList(fullList)
            updateCounters(fullList)
            attachSearchFilter()
            return
        }

        // 3) Carga real para la fecha seleccionada
        updateDateLabel()
        reloadForSelectedDate(grade)

        // 4) Filtro en vivo (código o nombre)
        attachSearchFilter()

        // 5) Navegación por fecha
        btnPrev.setOnClickListener {
            cal.add(Calendar.DAY_OF_MONTH, -1)
            updateDateLabel()
            reloadForSelectedDate(grade)
        }
        btnNext.setOnClickListener {
            cal.add(Calendar.DAY_OF_MONTH, +1)
            updateDateLabel()
            reloadForSelectedDate(grade)
        }
    }

    // ====================== FECHA ======================

    private fun selectedSpanishKey(): String {
        val fmt = SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es","PE"))
        return fmt.format(cal.time) // p.ej. "19 de octubre de 2025"
    }

    private fun updateDateLabel() {
        tvDate.text = selectedSpanishKey()
    }

    private fun reloadForSelectedDate(grade: String) {
        loadStudentsAndDateAttendance(grade, cal)
    }

    // ====================== CARGA DE DATOS ======================

    private fun loadStudentsAndDateAttendance(grade: String, selectedCal: Calendar) {
        // a) Estudiantes del grado
        db.collection("estudiantes")
            .whereEqualTo("grado", grade)   // se asume string "1".."6"
            .get()
            .addOnSuccessListener { qs ->
                val students = qs.documents.map { d ->
                    val code = d.getString("id") ?: d.id
                    val nombres = d.getString("nombres") ?: ""
                    val apellidos = d.getString("apellidos") ?: ""
                    val name = if (apellidos.isBlank() && nombres.isBlank()) "—" else "$apellidos, $nombres"
                    code to TeacherStudentUI(code, name, "—")
                }.toMap().toMutableMap()

                // b) Asistencias del grado → filtramos por el DÍA SELECCIONADO
                db.collection("asistencias_globales")
                    .whereEqualTo("grado", grade)
                    .get()
                    .addOnSuccessListener { attSnap ->
                        attSnap.documents.forEach { d ->
                            val fechaAny = d.get("fecha") // puede ser String o Timestamp
                            if (isSameDay(fechaAny, selectedCal)) {
                                val code = d.getString("id_estudiante") ?: return@forEach
                                val estadoCanon = canonicalStatus(d.getString("estado"))
                                students[code] = students[code]?.copy(status = estadoCanon) ?: return@forEach
                            }
                        }

                        fullList = students.values.sortedBy { it.code }
                        adapter.submitList(fullList)
                        updateCounters(fullList)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error asistencias: ${e.message}", Toast.LENGTH_SHORT).show()
                        fullList = students.values.sortedBy { it.code }
                        adapter.submitList(fullList)
                        updateCounters(fullList)
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error estudiantes: ${e.message}", Toast.LENGTH_SHORT).show()
                // Fallback a demo para poder ver UI
                fullList = mockStudentsData()
                adapter.submitList(fullList)
                updateCounters(fullList)
            }
    }

    // ====================== COMPARACIÓN DE FECHA ROBUSTA ======================

    /** Acepta Timestamp o String local como "19 de octubre de 2025, 12:53:03 a.m. UTC-5" */
    private fun isSameDay(field: Any?, selectedCal: Calendar): Boolean {
        try {
            when (field) {
                is Timestamp -> {
                    val cal2 = Calendar.getInstance()
                    cal2.time = field.toDate()
                    return cal2.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
                            cal2.get(Calendar.DAY_OF_YEAR) == selectedCal.get(Calendar.DAY_OF_YEAR)
                }
                is Date -> {
                    val cal2 = Calendar.getInstance()
                    cal2.time = field
                    return cal2.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
                            cal2.get(Calendar.DAY_OF_YEAR) == selectedCal.get(Calendar.DAY_OF_YEAR)
                }
                is String -> {
                    // Nos quedamos con el “día en español” y comparamos con startsWith para tolerancia
                    val dayPart = field.substringBefore(",").trim().lowercase()
                    val sel = SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es","PE"))
                        .format(selectedCal.time)
                        .lowercase()
                    return dayPart == sel || field.trim().lowercase().startsWith(sel)
                }
                else -> return false
            }
        } catch (_: Exception) { }
        return false
    }

    // ====================== BÚSQUEDA Y CONTADORES ======================

    private fun attachSearchFilter() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilter(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun applyFilter(query: String) {
        val q = normalize(query.trim())
        val filtered = if (q.isEmpty()) fullList else fullList.filter {
            val code = normalize(it.code)
            val name = normalize(it.name)
            code.contains(q) || name.contains(q)
        }
        adapter.submitList(filtered)
        updateCounters(filtered)
    }

    private fun updateCounters(list: List<TeacherStudentUI>) {
        val p = list.count { it.status.equals("puntual", true) }
        val t = list.count { it.status.equals("tarde", true) }
        val f = list.count { it.status.equals("falta", true) }
        tvP.text = p.toString()
        tvT.text = t.toString()
        tvF.text = f.toString()
    }

    // ====================== HELPERS ======================

    private fun canonicalStatus(raw: String?): String {
        val r = (raw ?: "—").trim().lowercase()
        val map = mapOf(
            // Puntual
            "puntual" to "puntual", "presente" to "puntual", "present" to "puntual", "on_time" to "puntual",
            // Tarde
            "tarde" to "tarde", "tardanza" to "tarde", "late" to "tarde",
            // Falta
            "falta" to "falta", "ausente" to "falta", "absent" to "falta"
        )
        return map[r] ?: "—"
    }

    private fun normalize(s: String): String {
        val tmp = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
        return tmp.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }

    // --- MOCK para modo demo ---
    private fun mockStudentsData() = listOf(
        TeacherStudentUI("20230001", "Linares, Marco José", "puntual"),
        TeacherStudentUI("20230002", "Pérez, Ana María", "tarde"),
        TeacherStudentUI("20230003", "Gómez, Luis", "falta"),
        TeacherStudentUI("20230004", "Quispe, Rocío", "puntual"),
        TeacherStudentUI("20230005", "Torres, Diego", "puntual"),
    )
}
