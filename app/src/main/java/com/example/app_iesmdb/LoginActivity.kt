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

        // Bypass para pruebas (entra directo al director)
        if (resources.getBoolean(R.bool.auth_bypass)) {
            val i = Intent(this, MainActivity::class.java)
                .putExtra("dest_res_id", R.id.directorHomeFragment)
            startActivity(i)
            finish()
            return
        }

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

            val email = usernameToEmail(user)

            auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid
                    if (uid.isNullOrBlank()) {
                        toast("Error interno (UID vacío)")
                        auth.signOut()
                        return@addOnSuccessListener
                    }

                    db.collection("users").document(uid)
                        .get()
                        .addOnSuccessListener { doc ->
                            // 1) Si NO existe el documento => cuenta deshabilitada / no configurada
                            if (!doc.exists()) {
                                toast("Tu cuenta no está configurada. Contacta al administrador.")
                                auth.signOut()
                                return@addOnSuccessListener
                            }

                            // 2) Leer rol
                            val role = (doc.getString("role")
                                ?: doc.getString("rol")
                                ?: ""
                                    ).lowercase()

                            Log.d("LOGIN_DEBUG", "Rol detectado: $role")

                            if (role.isBlank()) {
                                toast("Tu rol no está configurado correctamente. Contacta al administrador.")
                                auth.signOut()
                                return@addOnSuccessListener
                            }

                            // 3) Enrutar según rol
                            when (role) {
                                "director", "administrativo" -> {
                                    startActivity(Intent(this, MainActivity::class.java))
                                    finish()
                                }
                                "tutor", "profesor" -> {
                                    val grado = doc.getString("grado") ?: ""
                                    val intent = Intent(this, MainTeacherActivity::class.java)
                                    intent.putExtra("GRADE", grado)
                                    startActivity(intent)
                                    finish()
                                }
                                "auxiliar" -> {
                                    startActivity(Intent(this, MainAuxiliarActivity::class.java))
                                    finish()
                                }
                                else -> {
                                    // Rol desconocido -> bloquear
                                    toast("Rol no permitido. Contacta al administrador.")
                                    auth.signOut()
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            toast(e.message ?: "No se pudo leer el rol del usuario")
                            auth.signOut()
                        }
                }
                .addOnFailureListener { e ->
                    toast(e.message ?: "Error al iniciar sesión")
                }
        }
    }

    private fun usernameToEmail(username: String) =
        "${username}@demo.local"   // mismo dominio sintético que usas al crear usuarios

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
