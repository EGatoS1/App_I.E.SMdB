package com.example.app_iesmdb

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController

        val bottom = findViewById<BottomNavigationView>(R.id.bottomBarDirector)
        bottom.setupWithNavController(navController)

        bottom.setOnItemSelectedListener { item ->
            when (item.itemId) {

                // Cerrar sesión
                R.id.action_logout_director -> {
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    true
                }

                // Inicio: siempre volver al menú del Director
                R.id.directorHomeFragment -> {
                    navController.popBackStack(R.id.directorHomeFragment, false)
                    true
                }

                // Credenciales u otros destinos del graph
                else -> NavigationUI.onNavDestinationSelected(item, navController)
            }
        }

        // Si re-tocas "Inicio", también fuerza volver al menú
        bottom.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.directorHomeFragment) {
                navController.popBackStack(R.id.directorHomeFragment, false)
            }
        }
    }
}
