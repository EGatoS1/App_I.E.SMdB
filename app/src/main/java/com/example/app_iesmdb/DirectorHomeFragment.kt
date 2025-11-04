package com.example.app_iesmdb

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView

class DirectorHomeFragment : Fragment(R.layout.fragment_director_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar (si la usas para algo, aquí podrías configurar cosas extras)
        // Si NO quieres flecha atrás aquí, no le pongas navigationIcon en el XML

        // Cards del menú
        val cardUsers = view.findViewById<MaterialCardView>(R.id.cardUsers)
        val cardGrades = view.findViewById<MaterialCardView>(R.id.cardGrades)
        val cardDashboard = view.findViewById<MaterialCardView>(R.id.cardDashboard)

        cardUsers.setOnClickListener {
            // Ir a la pantalla de CRUD de usuarios
            findNavController().navigate(R.id.usersFragment)
        }

        cardGrades.setOnClickListener {
            findNavController().navigate(R.id.gradesFragment)
        }

        cardDashboard.setOnClickListener {
            findNavController().navigate(R.id.dashboardFragment)
        }
    }
}
