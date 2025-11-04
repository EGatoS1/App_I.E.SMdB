package com.example.app_iesmdb

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.Normalizer
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    // Pie chart
    private lateinit var pieView: AttendancePieView


    // Firestore
    private val db by lazy { FirebaseFirestore.getInstance() }

    // Views de fechas
    private lateinit var etStartDate: TextInputEditText
    private lateinit var etEndDate: TextInputEditText

    // Leyenda
    private lateinit var tvLegendPresent: TextView
    private lateinit var tvLegendLate: TextView
    private lateinit var tvLegendAbsent: TextView

    // Barras por grado (1..6)
    private lateinit var barPresentViews: List<View>
    private lateinit var barLateViews: List<View>
    private lateinit var barAbsentViews: List<View>

    // Rango de fechas seleccionado
    private val startCal: Calendar = Calendar.getInstance()
    private val endCal: Calendar = Calendar.getInstance()

    // Formato de fecha para mostrar en los campos
    private val uiDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE"))

    // Contadores por grado
    data class Counts(var p: Int = 0, var t: Int = 0, var f: Int = 0)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Referencias a vistas ---
        etStartDate = view.findViewById(R.id.etStartDate)
        etEndDate = view.findViewById(R.id.etEndDate)

        tvLegendPresent = view.findViewById(R.id.tvLegendPresent)
        tvLegendLate = view.findViewById(R.id.tvLegendLate)
        tvLegendAbsent = view.findViewById(R.id.tvLegendAbsent)
        pieView = view.findViewById(R.id.viewPieChart)


        barPresentViews = listOf(
            view.findViewById(R.id.bar1Present),
            view.findViewById(R.id.bar2Present),
            view.findViewById(R.id.bar3Present),
            view.findViewById(R.id.bar4Present),
            view.findViewById(R.id.bar5Present),
            view.findViewById(R.id.bar6Present),
        )

        barLateViews = listOf(
            view.findViewById(R.id.bar1Late),
            view.findViewById(R.id.bar2Late),
            view.findViewById(R.id.bar3Late),
            view.findViewById(R.id.bar4Late),
            view.findViewById(R.id.bar5Late),
            view.findViewById(R.id.bar6Late),
        )

        barAbsentViews = listOf(
            view.findViewById(R.id.bar1Absent),
            view.findViewById(R.id.bar2Absent),
            view.findViewById(R.id.bar3Absent),
            view.findViewById(R.id.bar4Absent),
            view.findViewById(R.id.bar5Absent),
            view.findViewById(R.id.bar6Absent),
        )

        // Rango por defecto: últimos 7 días (hoy incluido)
        endCal.timeInMillis = System.currentTimeMillis()
        startCal.timeInMillis = endCal.timeInMillis
        startCal.add(Calendar.DAY_OF_MONTH, -6)

        updateDateFields()

        etStartDate.setOnClickListener { showDatePicker(isStart = true) }
        etEndDate.setOnClickListener { showDatePicker(isStart = false) }

        // Cargar dashboard inicial
        reloadDashboard()
    }

    // ================== FECHAS ==================

    private fun updateDateFields() {
        etStartDate.setText(uiDateFormat.format(startCal.time))
        etEndDate.setText(uiDateFormat.format(endCal.time))
    }

    private fun showDatePicker(isStart: Boolean) {
        val cal = if (isStart) startCal else endCal

        val dialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                // Asegurar que inicio <= fin
                if (startCal.timeInMillis > endCal.timeInMillis) {
                    if (isStart) {
                        endCal.timeInMillis = startCal.timeInMillis
                    } else {
                        startCal.timeInMillis = endCal.timeInMillis
                    }
                }

                updateDateFields()
                reloadDashboard()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )

        dialog.show()
    }

    // ================== FIRESTORE + CÁLCULOS ==================

    private fun reloadDashboard() {
        // Normalizamos inicio y fin a 00:00 y 23:59
        val startDay = (startCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endDay = (endCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        // Contadores globales
        var totalPresent = 0
        var totalLate = 0
        var totalAbsent = 0

        // Por grado: 1..6
        val gradesMap = mutableMapOf<String, Counts>()
        for (g in 1..6) {
            gradesMap[g.toString()] = Counts()
        }

        // ❗ Igual que en el tutor: traemos todo y filtramos por fecha en código
        db.collection("asistencias_globales")
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    updateLegend(0, 0, 0)
                    updateBars(gradesMap)
                    return@addOnSuccessListener
                }

                for (doc in snap.documents) {
                    val fechaAny: Any? =
                        doc.get("timestamp") ?: doc.get("fecha") // usamos lo que exista

                    if (!isWithinRange(fechaAny, startDay, endDay)) {
                        continue
                    }

                    val rawEstado = doc.getString("estado")
                    val estado = canonicalStatus(rawEstado) // puntual / tarde / falta / "—"

                    val grado = doc.getString("grado") ?: "?"
                    val counts = gradesMap.getOrPut(grado) { Counts() }

                    when (estado) {
                        "puntual" -> {
                            totalPresent++
                            counts.p++
                        }
                        "tarde" -> {
                            totalLate++
                            counts.t++
                        }
                        "falta" -> {
                            totalAbsent++
                            counts.f++
                        }
                    }
                }

                updateLegend(totalPresent, totalLate, totalAbsent)
                updateBars(gradesMap)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error cargando asistencias: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // ================== ACTUALIZAR UI ==================

    private fun updateLegend(present: Int, late: Int, absent: Int) {
        val total = present + late + absent
        if (total == 0) {
            tvLegendPresent.text = "Asistencia (0%)"
            tvLegendLate.text = "Tardanza (0%)"
            tvLegendAbsent.text = "Ausencia (0%)"

            // Pastel vacío
            pieView.setValues(0f, 0f, 0f)
            return
        }

        fun pct(v: Int) = (v * 100.0 / total)

        val pPct = pct(present)
        val tPct = pct(late)
        val fPct = pct(absent)

        tvLegendPresent.text =
            String.format(Locale.getDefault(), "Asistencia (%.0f%%)", pPct)
        tvLegendLate.text =
            String.format(Locale.getDefault(), "Tardanza (%.0f%%)", tPct)
        tvLegendAbsent.text =
            String.format(Locale.getDefault(), "Ausencia (%.0f%%)", fPct)

        // 🔹 Actualizamos el pastel con los mismos porcentajes
        pieView.setValues(pPct.toFloat(), tPct.toFloat(), fPct.toFloat())
    }


    /** Actualiza las barras apiladas de cada grado usando los contadores */
    private fun updateBars(gradesMap: Map<String, Counts>) {
        for (gradeIndex in 1..6) {
            val key = gradeIndex.toString()
            val counts = gradesMap[key] ?: Counts()

            val p = counts.p
            val t = counts.t
            val f = counts.f
            val total = p + t + f

            val presentView = barPresentViews[gradeIndex - 1]
            val lateView = barLateViews[gradeIndex - 1]
            val absentView = barAbsentViews[gradeIndex - 1]

            if (total == 0) {
                // Sin datos → barra en cero
                setWeights(presentView, 0f)
                setWeights(lateView, 0f)
                setWeights(absentView, 0f)
            } else {
                // Conteos como pesos
                setWeights(presentView, p.toFloat())
                setWeights(lateView, t.toFloat())
                setWeights(absentView, f.toFloat())
            }
        }
    }

    private fun setWeights(view: View, weight: Float) {
        val lp = view.layoutParams as? android.widget.LinearLayout.LayoutParams
        if (lp != null) {
            lp.height = 0
            lp.weight = weight
            view.layoutParams = lp
        }
    }

    // ================== HELPERS ==================

    /** Normaliza el estado a puntual / tarde / falta / "—" */
    private fun canonicalStatus(raw: String?): String {
        val r = normalize((raw ?: "—").trim())
        return when (r) {
            "puntual", "presente", "present", "ontime", "on_time" -> "puntual"
            "tarde", "tardanza", "late" -> "tarde"
            "falta", "ausente", "absent" -> "falta"
            else -> "—"
        }
    }

    private fun normalize(s: String): String {
        val tmp = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
        return tmp.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }

    /**
     * Devuelve true si la fecha del documento cae entre startDay y endDay (inclusive).
     * Acepta:
     *  - Timestamp
     *  - Date
     *  - String "yyyy-MM-dd"
     *  - String largo "3 de noviembre de 2025, 12:53:03 a.m. UTC-5"
     */
    private fun isWithinRange(field: Any?, startDay: Calendar, endDay: Calendar): Boolean {
        try {
            val calDoc = Calendar.getInstance()

            when (field) {
                is Timestamp -> {
                    calDoc.time = field.toDate()
                }
                is Date -> {
                    calDoc.time = field
                }
                is String -> {
                    val s = field.trim()

                    // Intento 1: yyyy-MM-dd
                    val fmtIso = SimpleDateFormat("yyyy-MM-dd", Locale("es", "PE"))
                    try {
                        calDoc.time = fmtIso.parse(s)!!
                    } catch (_: ParseException) {
                        // Intento 2: "d 'de' MMMM 'de' yyyy" al inicio
                        val dayPart = s.substringBefore(",").trim()
                        val fmtEs = SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es", "PE"))
                        try {
                            calDoc.time = fmtEs.parse(dayPart)!!
                        } catch (_: Exception) {
                            return false
                        }
                    }
                }
                else -> return false
            }

            // Normalizamos el día del documento a 00:00 para comparar por día
            val c = (calDoc.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            return !c.before(startDay) && !c.after(endDay)
        } catch (_: Exception) {
        }
        return false
    }
}
