package com.example.app_iesmdb

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DirectorHomeFragment : Fragment(R.layout.fragment_director_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // --- Tarjetas del menú ---
        view.findViewById<View>(R.id.cardGrades).setOnClickListener {
            findNavController().navigate(R.id.gradesFragment)
        }

        view.findViewById<View>(R.id.cardDashboard).setOnClickListener {
            findNavController().navigate(R.id.dashboardFragment)
        }

    }
}
