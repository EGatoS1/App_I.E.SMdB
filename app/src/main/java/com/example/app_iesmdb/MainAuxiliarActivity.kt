package com.example.app_iesmdb

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.ui.NavigationUI


class MainAuxiliarActivity : AppCompatActivity(R.layout.activity_main_auxiliar) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_aux) as NavHostFragment
        val navController = navHost.navController

        val bottom = findViewById<BottomNavigationView>(R.id.bottomBarAux)
        bottom.setupWithNavController(navController)

        bottom.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.action_logout_aux) {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            } else {
                NavigationUI.onNavDestinationSelected(item, navController)
            }
        }
    }
}
