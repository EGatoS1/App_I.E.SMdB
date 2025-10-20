package com.example.app_iesmdb

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        
        // Ir directo al flujo del Director para probar UI sin login
        if (resources.getBoolean(R.bool.auth_bypass)) {

            val i = Intent(this, MainActivity::class.java)
                .putExtra("dest_res_id", R.id.directorHomeFragment) // opcional: a qué pantalla ir
            startActivity(i)
            finish()
            return
        }


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
                    // Login OK: ahora decidimos por rol en Firestore
                    val uid = it.user!!.uid
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { doc ->
                            // Acepta 'role' o 'rol' según como lo hayan guardado
                            val role = (doc.getString("role") ?: doc.getString("rol") ?: "sin_rol").lowercase()
                            Log.d("LOGIN_DEBUG", "Rol detectado: $role")

                            Toast.makeText(this, "Rol detectado: $role", Toast.LENGTH_SHORT).show()

                            when (role) {
                                "director", "administrativo" -> {
                                    startActivity(Intent(this, MainActivity::class.java))
                                }
                                "tutor", "profesor" -> {
                                    val grado = doc.getString("grado") ?: ""
                                    startActivity(Intent(this, MainTeacherActivity::class.java).apply {
                                        putExtra("GRADE", grado)
                                    })
                                }
                                "auxiliar" -> {
                                    startActivity(Intent(this, MainAuxiliarActivity::class.java))
                                }
                                else -> {
                                    // Fallback seguro
                                    startActivity(Intent(this, MainActivity::class.java))
                                }
                            }
                            finish()
                        }
                        .addOnFailureListener { e ->
                            toast(e.message ?: "No se pudo leer el rol del usuario")
                        }

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
