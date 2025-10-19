package com.example.app_iesmdb

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView

class GradesFragment : Fragment(R.layout.fragment_grades) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<MaterialToolbar>(R.id.toolbarGrades)
            ?.setNavigationOnClickListener {
                // Vuelve a la pantalla anterior
                findNavController().popBackStack()
            }

        // Vincula las tarjetas de cada grado
        val grades = listOf(
            R.id.cardGrade1 to "Primer Grado",
            R.id.cardGrade2 to "Segundo Grado",
            R.id.cardGrade3 to "Tercer Grado",
            R.id.cardGrade4 to "Cuarto Grado",
            R.id.cardGrade5 to "Quinto Grado",
            R.id.cardGrade6 to "Sexto Grado"
        )

        // Configura cada click
        grades.forEach { (id, name) ->
            view.findViewById<MaterialCardView>(id)?.setOnClickListener {
                val bundle = Bundle().apply { putString("gradeName", name) }
                findNavController().navigate(R.id.gradeDetailFragment, bundle)
            }
        }
    }
}
