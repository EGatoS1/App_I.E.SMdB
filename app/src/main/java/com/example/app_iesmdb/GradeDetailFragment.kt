package com.example.app_iesmdb

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app_iesmdb.attendance.Attendance
import com.example.app_iesmdb.attendance.AttendanceAdapter
import com.example.app_iesmdb.attendance.Status
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar

class GradeDetailFragment : Fragment(R.layout.fragment_grade_detail) {

    private lateinit var rv: RecyclerView
    private lateinit var adapter: AttendanceAdapter

    private lateinit var etStart: TextInputEditText
    private lateinit var etEnd: TextInputEditText
    private lateinit var etCode: TextInputEditText

    private lateinit var tvPresent: TextView
    private lateinit var tvAbsent: TextView
    private lateinit var tvLate: TextView
    private lateinit var tvReportFor: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar
        val toolbar = view.findViewById<com.google.android.material.appbar.MaterialToolbar>(
            R.id.toolbarGradeDetail
        )
        val gradeName = arguments?.getString("gradeName") ?: "Quinto Grado"
        toolbar.title = gradeName
        toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        etStart = view.findViewById(R.id.etStartDate)
        etEnd = view.findViewById(R.id.etEndDate)
        etCode = view.findViewById(R.id.etCode)

        tvPresent = view.findViewById(R.id.tvCountPresent)
        tvAbsent  = view.findViewById(R.id.tvCountAbsent)
        tvLate    = view.findViewById(R.id.tvCountLate)
        tvReportFor = view.findViewById(R.id.tvReportFor)

        rv = view.findViewById(R.id.rvAttendance)
        adapter = AttendanceAdapter(mutableListOf())
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        // Dummy: valores iniciales
        etStart.setText("01/07/2024")
        etEnd.setText("15/07/2024")
        etCode.setText("20230001")
        tvReportFor.text = "Reporte para: 20230001"

        // Carga inicial
        val sample = fakeData()
        renderStats(sample)
        adapter.submit(sample)

        // Date pickers (UI)
        etStart.setOnClickListener { showDatePicker { etStart.setText(it) } }
        etEnd.setOnClickListener   { showDatePicker { etEnd.setText(it) } }

        // Botón Consultar
        view.findViewById<View>(R.id.btnConsultar).setOnClickListener {
            // Por ahora recarga dummy filtrando simple por código (si coincide).
            val code = etCode.text?.toString()?.trim().orEmpty()
            val data = if (code.isBlank()) sample else sample.filter { it.studentCode == code }
            renderStats(data)
            adapter.submit(data)
            if (code.isNotBlank()) tvReportFor.text = "Reporte para: $code"
        }
    }

    private fun renderStats(list: List<Attendance>) {
        val p = list.count { it.status == Status.PRESENTE }
        val a = list.count { it.status == Status.AUSENTE }
        val t = list.count { it.status == Status.TARDANZA }
        tvPresent.text = p.toString()
        tvAbsent.text  = a.toString()
        tvLate.text    = t.toString()
    }

    private fun showDatePicker(onPick: (String)->Unit) {
        val c = Calendar.getInstance()
        DatePickerDialog(requireContext(),
            { _, y, m, d -> onPick(String.format("%02d/%02d/%04d", d, m+1, y)) },
            c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Datos de muestra (idénticos al mock)
    private fun fakeData(): List<Attendance> = listOf(
        Attendance("01/07/2024","20230001", Status.PRESENTE),
        Attendance("02/07/2024","20230001", Status.PRESENTE),
        Attendance("03/07/2024","20230001", Status.TARDANZA),
        Attendance("04/07/2024","20230001", Status.AUSENTE),
        Attendance("05/07/2024","20230001", Status.PRESENTE)
    )
}
