package com.example.app_iesmdb

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController   // <-- IMPORT CLAVE

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHost =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController

        val bottom = findViewById<BottomNavigationView>(R.id.bottomBarDirector)

        // Enlaza bottom bar con el navController (extensión KTX)
        bottom.setupWithNavController(navController)

        // Si quieres interceptar logout:
        bottom.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_logout -> {
                    com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle("Cerrar sesión")
                        .setMessage("¿Seguro que quieres cerrar sesión?")
                        .setPositiveButton("Sí") { _, _ -> finish() }
                        .setNegativeButton("Cancelar", null)
                        .show()
                    true
                }
                else -> {
                    // Deja que la extensión gestione la navegación
                    // (devuelve false aquí para que el listener por defecto actúe)
                    false
                }
            }
        }
    }
}
