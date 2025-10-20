package com.example.app_iesmdb.teacher

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.app_iesmdb.R
import androidx.navigation.fragment.findNavController



class TeacherHomeFragment : Fragment(R.layout.fragment_teacher_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.cardReports).setOnClickListener {
            findNavController().navigate(R.id.teacherReportsFragment)
        }
        view.findViewById<View>(R.id.cardStudents).setOnClickListener {
            findNavController().navigate(R.id.teacherStudentsFragment)
        }
    }
}
