package com.example.app_iesmdb.teacher

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app_iesmdb.R
import com.example.app_iesmdb.attendance.Attendance
import com.example.app_iesmdb.attendance.AttendanceAdapter
import com.example.app_iesmdb.attendance.Status
import com.example.app_iesmdb.databinding.FragmentTeacherReportsBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

class TeacherReportsFragment : Fragment(R.layout.fragment_teacher_reports) {

    private var _binding: FragmentTeacherReportsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AttendanceAdapter

    // Rango de fechas
    private var startDate: LocalDate = LocalDate.now().minusDays(14)
    private var endDate: LocalDate = LocalDate.now()
    private val zone: ZoneId = ZoneId.systemDefault()
    private val uiFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    // Firestore
    private val db by lazy { Firebase.firestore }

    // Heurística simple para detectar códigos (N00000001 o 20230001)
    private val codeRegex: Pattern = Pattern.compile("^[A-Za-z]?[0-9]{5,}$")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTeacherReportsBinding.bind(view)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        renderDates()
        binding.etStartDate.setOnClickListener { openDatePicker(isStart = true) }
        binding.etEndDate.setOnClickListener { openDatePicker(isStart = false) }

        adapter = AttendanceAdapter(mutableListOf())
        binding.rvDetalle.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDetalle.adapter = adapter
        binding.rvDetalle.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )

        // Buscar al presionar "buscar" en el teclado
        binding.etStudentCode.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                binding.btnConsultar.performClick()
                true
            } else false
        }

        binding.btnConsultar.setOnClickListener {
            if (startDate.isAfter(endDate)) {
                Toast.makeText(requireContext(), "La fecha de inicio no puede ser mayor que la de fin.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val input = binding.etStudentCode.text?.toString()?.trim().orEmpty()
            if (input.isEmpty()) {
                binding.etStudentCode.error = "Ingresa código o nombre/apellido"
                binding.etStudentCode.requestFocus()
                return@setOnClickListener
            }

            val grade = getTutorGrade() ?: run {
                Toast.makeText(requireContext(), "No se encontró el grado del tutor.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            resolveStudentWithinGrade(input, grade) { code, display ->
                binding.tvReportFor.text = "Reporte para: $display"
                loadAttendanceByCodeAndRange(code)
            }
        }
    }

    /** Obtiene el grado del tutor desde SavedStateHandle o Intent */
    private fun getTutorGrade(): String? {
        val raw = findNavController().currentBackStackEntry?.savedStateHandle?.get<Any?>("GRADE")
            ?: requireActivity().intent.getStringExtra("GRADE")

        return when (raw) {
            is String -> raw.trim()
            is Number -> raw.toInt().toString()
            else -> (raw?.toString()?.trim())
        }?.takeIf { it.isNotBlank() }
    }

    /**
     * Resuelve un estudiante por código o nombre/apellido **dentro del grado**.
     * - Si el input parece CÓDIGO: busca doc y valida que su 'grado' == grade.
     * - Si es TEXTO: hace prefijo por apellidos/nombres y luego filtra por grade (cliente),
     *   mostrando diálogo si hay varios candidatos.
     */
    private fun resolveStudentWithinGrade(
        input: String,
        grade: String,
        onResolved: (code: String, display: String) -> Unit
    ) {
        val trimmed = input.trim()

        // 1) Código
        if (codeRegex.matcher(trimmed).matches()) {
            // Intenta por docId
            db.collection("estudiantes").document(trimmed).get()
                .addOnSuccessListener { d ->
                    if (d.exists()) {
                        val g = asGradeString(d.get("grado"))
                        if (g == grade) {
                            val nombre = "${d.getString("apellidos") ?: ""}, ${d.getString("nombres") ?: ""}".trim().trim(',')
                            onResolved(trimmed, "$trimmed — $nombre")
                        } else {
                            binding.etStudentCode.error = "El código pertenece al grado $g, no a $grade."
                        }
                    } else {
                        // Intenta por campo 'id'
                        db.collection("estudiantes").whereEqualTo("id", trimmed).limit(1).get()
                            .addOnSuccessListener { qs ->
                                val x = qs.documents.firstOrNull()
                                if (x != null) {
                                    val g = asGradeString(x.get("grado"))
                                    if (g == grade) {
                                        val code = x.getString("id") ?: trimmed
                                        val nombre = "${x.getString("apellidos") ?: ""}, ${x.getString("nombres") ?: ""}".trim().trim(',')
                                        onResolved(code, "$code — $nombre")
                                    } else {
                                        binding.etStudentCode.error = "El código pertenece al grado $g, no a $grade."
                                    }
                                } else {
                                    binding.etStudentCode.error = "No se encontró estudiante con código $trimmed"
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(), "Error buscando código: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error buscando código: ${e.message}", Toast.LENGTH_LONG).show()
                }
            return
        }

        // 2) Nombre / Apellido (prefijo). Hacemos dos consultas y luego filtramos por grado del tutor en cliente.
        val prefix = trimmed.replace(Regex("\\s+"), " ").trim()
        if (prefix.length < 2) {
            binding.etStudentCode.error = "Escribe al menos 2 letras del nombre o apellido"
            binding.etStudentCode.requestFocus()
            return
        }
        val end = prefix + "\uf8ff"

        val qApe = db.collection("estudiantes")
            .orderBy("apellidos")
            .startAt(prefix).endAt(end).limit(15)
        val qNom = db.collection("estudiantes")
            .orderBy("nombres")
            .startAt(prefix).endAt(end).limit(15)

        qApe.get().continueWithTask { apeSnap ->
            val apeDocs = apeSnap.result?.documents ?: emptyList()
            qNom.get().continueWith { nomSnap ->
                val nomDocs = nomSnap.result?.documents ?: emptyList()
                val merged = (apeDocs + nomDocs)
                    .distinctBy { it.id }
                    .mapNotNull { d ->
                        val g = asGradeString(d.get("grado"))
                        if (g == grade) {
                            val code = d.getString("id") ?: d.id
                            val nombre = "${d.getString("apellidos") ?: ""}, ${d.getString("nombres") ?: ""}".trim().trim(',')
                            code to nombre
                        } else null
                    }
                merged
            }
        }.addOnSuccessListener { candidates ->
            if (candidates.isEmpty()) {
                binding.etStudentCode.error = "Sin coincidencias en el grado $grade"
                return@addOnSuccessListener
            }
            if (candidates.size == 1) {
                val (code, nombre) = candidates.first()
                onResolved(code, "$code — $nombre")
            } else {
                val labels = candidates.map { (c, n) -> "$c — $n" }.toTypedArray()
                AlertDialog.Builder(requireContext())
                    .setTitle("Selecciona estudiante")
                    .setItems(labels) { _, which ->
                        val (code, nombre) = candidates[which]
                        onResolved(code, "$code — $nombre")
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Error buscando estudiantes: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ================== Consulta de asistencias (dentro del rango) ==================
    private fun loadAttendanceByCodeAndRange(studentCode: String) {
        setKpis(0, 0, 0)
        adapter.submit(emptyList())

        db.collection("asistencias_globales")
            .whereEqualTo("id_estudiante", studentCode)
            .get()
            .addOnSuccessListener { snap ->
                val items = mutableListOf<Pair<LocalDate, Status>>()

                for (d in snap.documents) {
                    val estadoStr = (d.getString("estado") ?: "").trim().uppercase(Locale.ROOT)
                    val date: LocalDate? = when (val raw = d.get("fecha")) {
                        is Timestamp -> {
                            Instant.ofEpochSecond(raw.seconds, raw.nanoseconds.toLong())
                                .atZone(zone).toLocalDate()
                        }
                        is String -> parseSpanishDateToLocalDate(raw)
                        else -> null
                    }
                    if (date != null && !date.isBefore(startDate) && !date.isAfter(endDate)) {
                        val status = when (estadoStr) {
                            "PUNTUAL", "PRESENTE" -> Status.PRESENTE
                            "TARDE", "TARDANZA"   -> Status.TARDANZA
                            else                  -> Status.AUSENTE
                        }
                        items.add(date to status)
                    }
                }

                var asist = 0; var falt = 0; var tard = 0
                val sorted = items.sortedBy { it.first }.map { (date, status) ->
                    when (status) {
                        Status.PRESENTE -> asist++
                        Status.TARDANZA -> tard++
                        Status.AUSENTE  -> falt++
                    }
                    Attendance(
                        date = uiFormatter.format(date),
                        studentCode = studentCode,
                        status = status
                    )
                }

                setKpis(asist, falt, tard)
                adapter.submit(sorted)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error cargando asistencias: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // "24 de octubre de 2025, 12:00:00 a.m. UTC-5" -> 24/10/2025
    private fun parseSpanishDateToLocalDate(fechaStr: String): LocalDate? {
        val soloDia = fechaStr.substringBefore(",").trim()
        val localeEs = Locale("es", "PE")
        val f1 = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' uuuu", localeEs)
        return runCatching { LocalDate.parse(soloDia, f1) }
            .getOrElse { runCatching { LocalDate.parse(soloDia, DateTimeFormatter.ofPattern("dd/MM/uuuu")) }.getOrNull() }
    }

    private fun setKpis(asist: Int, falt: Int, tard: Int) {
        binding.tvCountAsistencias.text = asist.toString()
        binding.tvCountFaltas.text = falt.toString()
        binding.tvCountTardanzas.text = tard.toString()
    }

    private fun renderDates() {
        binding.etStartDate.setText(uiFormatter.format(startDate))
        binding.etEndDate.setText(uiFormatter.format(endDate))
    }

    private fun openDatePicker(isStart: Boolean) {
        val current = if (isStart) startDate else endDate
        val cal = Calendar.getInstance().apply {
            timeInMillis = current.atStartOfDay(zone).toInstant().toEpochMilli()
        }
        DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                val picked = LocalDate.of(y, m + 1, d)
                if (isStart) startDate = picked else endDate = picked
                renderDates()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    /** Convierte 'grado' (String o Number) a String "1","2",... */
    private fun asGradeString(value: Any?): String? = when (value) {
        is String -> value.trim()
        is Number -> value.toInt().toString()
        else -> value?.toString()?.trim()
    }?.takeIf { it.isNotBlank() }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
