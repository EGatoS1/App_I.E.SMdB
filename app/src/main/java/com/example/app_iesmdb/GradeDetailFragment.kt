package com.example.app_iesmdb

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GradeDetailFragment : Fragment(R.layout.fragment_grade_detail) {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var rv: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var tvP: TextView
    private lateinit var tvT: TextView
    private lateinit var tvF: TextView

    private lateinit var tvDate: TextView
    private lateinit var btnPrev: TextView
    private lateinit var btnNext: TextView
    private val cal = Calendar.getInstance()

    private lateinit var adapter: TeacherStudentsAdapter
    private var fullList: List<TeacherStudentUI> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbarGradeDetail)
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // 1) SIEMPRE inicializamos las vistas primero
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

        // 2) Obtenemos el nombre de grado desde los argumentos
        val gradeTitle: String = arguments?.getString("gradeName") ?: "Primer grado"
        val gradeKey = gradeKeyFromTitle(gradeTitle)

        if (gradeKey.isBlank()) {
            Toast.makeText(requireContext(), "Grado no válido: $gradeTitle", Toast.LENGTH_SHORT).show()
            return
        }

        // 3) Configuración de fecha inicial
        updateDateLabel()

        // 4) Carga de datos (mock o real)
        val useMock = resources.getBoolean(R.bool.use_mock_students)
        if (useMock) {
            fullList = mockStudentsData()
            adapter.submitList(fullList)
            updateCounters(fullList)
        } else {
            reloadForSelectedDate(gradeKey)
        }

        // 5) Filtro en vivo
        attachSearchFilter()

        // 6) Navegación de días
        btnPrev.setOnClickListener {
            cal.add(Calendar.DAY_OF_MONTH, -1)
            updateDateLabel()
            if (!useMock) reloadForSelectedDate(gradeKey)
        }

        btnNext.setOnClickListener {
            cal.add(Calendar.DAY_OF_MONTH, +1)
            updateDateLabel()
            if (!useMock) reloadForSelectedDate(gradeKey)
        }
    }

    // ====================== MAPEO NOMBRE → CLAVE BD ======================

    private fun gradeKeyFromTitle(title: String): String {
        return when (title.lowercase().trim()) {
            "primer grado"  -> "1"
            "segundo grado" -> "2"
            "tercer grado"  -> "3"
            "cuarto grado"  -> "4"
            "quinto grado"  -> "5"
            "sexto grado"   -> "6"
            else            -> title
        }
    }

    // ====================== FECHA ======================

    private fun selectedSpanishKey(): String {
        val fmt = SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es","PE"))
        return fmt.format(cal.time)
    }

    private fun updateDateLabel() {
        tvDate.text = selectedSpanishKey()
    }

    private fun reloadForSelectedDate(gradeKey: String) {
        loadStudentsAndDateAttendance(gradeKey, cal)
    }

    // ====================== CARGA DE DATOS ======================

    private fun loadStudentsAndDateAttendance(gradeKey: String, selectedCal: Calendar) {
        db.collection("estudiantes")
            .whereEqualTo("grado", gradeKey)
            .get()
            .addOnSuccessListener { qs ->
                val students = qs.documents.map { d ->
                    val code = d.getString("id") ?: d.id
                    val nombres = d.getString("nombres") ?: ""
                    val apellidos = d.getString("apellidos") ?: ""
                    val name = if (apellidos.isBlank() && nombres.isBlank()) "—" else "$apellidos, $nombres"
                    code to TeacherStudentUI(code, name, "—")
                }.toMap().toMutableMap()

                db.collection("asistencias_globales")
                    .whereEqualTo("grado", gradeKey)
                    .get()
                    .addOnSuccessListener { attSnap ->
                        attSnap.documents.forEach { d ->
                            val fechaAny = d.get("fecha")
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
                fullList = mockStudentsData()
                adapter.submitList(fullList)
                updateCounters(fullList)
            }
    }

    // ====================== COMPARACIÓN DE FECHA ======================

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
            "puntual" to "puntual", "presente" to "puntual", "present" to "puntual", "on_time" to "puntual",
            "tarde" to "tarde", "tardanza" to "tarde", "late" to "tarde",
            "falta" to "falta", "ausente" to "falta", "absent" to "falta"
        )
        return map[r] ?: "—"
    }

    private fun normalize(s: String): String {
        val tmp = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
        return tmp.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }

    private fun mockStudentsData() = listOf(
        TeacherStudentUI("20230001", "Linares, Marco José", "puntual"),
        TeacherStudentUI("20230002", "Pérez, Ana María", "tarde"),
        TeacherStudentUI("20230003", "Gómez, Luis", "falta"),
        TeacherStudentUI("20230004", "Quispe, Rocío", "puntual"),
        TeacherStudentUI("20230005", "Torres, Diego", "puntual"),
    )
}
