package com.example.app_iesmdb

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController

class MainAuxiliarActivity : AppCompatActivity(R.layout.activity_main_auxiliar) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_auxiliar) as NavHostFragment
        val navController = navHost.navController

        val bottom = findViewById<BottomNavigationView>(R.id.bottomBarAuxiliar)
        bottom.setupWithNavController(navController)

        bottom.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_credentials_aux -> {
                    navController.navigate(R.id.auxiliarCredentialsFragment)
                    true
                }
                R.id.action_home_aux -> {
                    navController.navigate(R.id.auxiliarHomeFragment)
                    true
                }
                R.id.action_logout_aux -> {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

    }
}
