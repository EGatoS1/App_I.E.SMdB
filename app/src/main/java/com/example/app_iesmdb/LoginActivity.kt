package com.example.app_iesmdb

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // Mantén tu manejo de insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etUsuario  = findViewById<TextInputEditText>(R.id.etUsuario)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnAcceder = findViewById<MaterialButton>(R.id.btnAcceder)

        btnAcceder.setOnClickListener {
            val user = etUsuario.text?.toString()?.trim()?.lowercase().orEmpty()
            val pass = etPassword.text?.toString()?.trim().orEmpty()

            if (user.isEmpty() || pass.isEmpty()) {
                toast("Completa usuario y contraseña")
                return@setOnClickListener
            }

            val email = usernameToEmail(user) // convierte "usuario" -> "usuario@demo.local"

            auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener {
                    // Login OK: redirige al MainActivity creado por defecto
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    toast(e.message ?: "Error al iniciar sesión")
                }
        }
    }

    private fun usernameToEmail(username: String) =
        "${username}@demo.local" // usa el mismo dominio sintético que usaste al crear el usuario

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
