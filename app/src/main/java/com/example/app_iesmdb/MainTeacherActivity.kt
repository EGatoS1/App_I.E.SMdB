package com.example.app_iesmdb

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.ui.NavigationUI

class MainTeacherActivity : AppCompatActivity(R.layout.activity_main_teacher) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_teacher) as NavHostFragment
        val navController = navHost.navController

        val bottom = findViewById<BottomNavigationView>(R.id.bottomBarTeacher)
        bottom.setupWithNavController(navController)

        bottom.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_logout_teacher -> {
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    true
                }
                // Hace que “Inicio” siempre regrese al menú del Tutor
                R.id.teacherHomeFragment -> {
                    navController.popBackStack(R.id.teacherHomeFragment, false)
                    true
                }
                else -> {
                    NavigationUI.onNavDestinationSelected(item, navController)
                }
            }
        }

// (Opcional) si re-tocas Inicio, también vuelve al menú
        bottom.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.teacherHomeFragment) {
                navController.popBackStack(R.id.teacherHomeFragment, false)
            }
        }


        // Manejo de logout (item no asociado a un destino)
        bottom.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.action_logout_teacher) {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            } else {
                // Navegar a destinos del graph
                NavigationUI.onNavDestinationSelected(item, navController)
            }
        }

        // Pasar el grado del tutor a los fragments
        val grade = intent.getStringExtra("GRADE") ?: ""
        navController.addOnDestinationChangedListener { _, _, _ ->
            navController.currentBackStackEntry?.savedStateHandle?.set("GRADE", grade)
        }
    }
}
