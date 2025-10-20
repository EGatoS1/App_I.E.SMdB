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
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class TeacherStudentsFragment : Fragment(R.layout.fragment_teacher_students) {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var adapter: TeacherStudentsAdapter
    private lateinit var rv: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var tvP: TextView
    private lateinit var tvT: TextView
    private lateinit var tvF: TextView

    private fun normalize(s: String): String {
        val tmp = java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD)
        return tmp.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }

    // Lista completa y filtrada
    private var fullList: List<TeacherStudentUI> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv = view.findViewById(R.id.rvStudents)
        etSearch = view.findViewById(R.id.etSearchCode)
        tvP = view.findViewById(R.id.tvCountPresent)
        tvT = view.findViewById(R.id.tvCountLate)
        tvF = view.findViewById(R.id.tvCountAbsent)

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

        // 2) MOCK: si el flag está activo, carga datos locales y sal
        val useMock = resources.getBoolean(R.bool.use_mock_students)
        if (useMock) {
            fullList = mockStudentsData()
            adapter.submitList(fullList)
            updateCounters(fullList)
            // filtro en vivo también aplica al mock
            attachSearchFilter()
            return
        }

        // 3) Cargar estudiantes + asistencias de HOY (desde Firestore)
        loadStudentsAndTodayAttendance(grade)

        // 4) Filtro por código en vivo
        attachSearchFilter()
    }

    private fun attachSearchFilter() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilter(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadStudentsAndTodayAttendance(grade: String) {
        // a) Estudiantes del grado
        db.collection("estudiantes")
            .whereEqualTo("grado", grade)   // suponemos grado como String
            .get()
            .addOnSuccessListener { qs ->
                val students = qs.documents.map { d ->
                    val code = d.getString("id") ?: d.id
                    val nombres = d.getString("nombres") ?: ""
                    val apellidos = d.getString("apellidos") ?: ""
                    val name = if (apellidos.isBlank() && nombres.isBlank()) "—" else "$apellidos, $nombres"
                    code to TeacherStudentUI(code, name, "—")
                }.toMap().toMutableMap()

                // b) Asistencias del grado → filtrar HOY en cliente
                db.collection("asistencias_globales")
                    .whereEqualTo("grado", grade)
                    .get()
                    .addOnSuccessListener { attSnap ->
                        val todayKey = todaySpanishKey()
                        attSnap.documents.forEach { d ->
                            val fechaStr = d.get("fecha")?.toString() ?: ""
                            if (isSameDaySpanish(fechaStr, todayKey)) {
                                val code = d.getString("id_estudiante") ?: return@forEach
                                val estado = d.getString("estado") ?: "—"
                                students[code] = students[code]?.copy(status = estado) ?: return@forEach
                            }
                        }

                        fullList = students.values.sortedBy { it.code }
                        adapter.submitList(fullList)
                        updateCounters(fullList)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error asistencias: ${e.message}", Toast.LENGTH_SHORT).show()
                        // Muestra al menos la lista de estudiantes
                        fullList = students.values.sortedBy { it.code }
                        adapter.submitList(fullList)
                        updateCounters(fullList)
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error estudiantes: ${e.message}", Toast.LENGTH_SHORT).show()
                // Fallback a MOCK si hay permisos insuficientes
                fullList = mockStudentsData()
                adapter.submitList(fullList)
                updateCounters(fullList)
            }
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

    private fun canonicalStatus(raw: String?): String {
        val r = (raw ?: "—").trim().lowercase()

        // Diccionario de sinónimos → valor canónico
        val map = mapOf(
            // puntual
            "puntual" to "puntual",
            "presente" to "puntual",
            "present" to "puntual",
            "on_time" to "puntual",

            // tarde
            "tarde" to "tarde",
            "tardanza" to "tarde",
            "late" to "tarde",

            // falta
            "falta" to "falta",
            "ausente" to "falta",
            "absent" to "falta"
        )
        return map[r] ?: "—"
    }


    // --- MOCK para modo demo ---
    private fun mockStudentsData() = listOf(
        TeacherStudentUI("20230001", "Linares, Marco José", "puntual"),
        TeacherStudentUI("20230002", "Pérez, Ana María", "tarde"),
        TeacherStudentUI("20230003", "Gómez, Luis", "falta"),
        TeacherStudentUI("20230004", "Quispe, Rocío", "puntual"),
        TeacherStudentUI("20230005", "Torres, Diego", "puntual"),
    )


    // --- Utilidades de fecha en español (para tu formato actual) ---
    private fun todaySpanishKey(): String {
        val fmt = SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es", "PE"))
        return fmt.format(Date()) // ej: "20 de octubre de 2025"
    }

    /** Ej: "19 de octubre de 2025, 12:53:03 a.m. UTC-5" → compara el día previo a la coma */
    private fun isSameDaySpanish(fechaStr: String, todayKey: String): Boolean {
        val dayPart = fechaStr.substringBefore(",").trim()
        return dayPart.equals(todayKey, ignoreCase = true)
    }
}
