package com.example.app_iesmdb

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TeacherCredentialsFragment : Fragment(R.layout.fragment_teacher_credentials) {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvNombre = view.findViewById<TextView>(R.id.tvNombreCompleto)
        val tvRol    = view.findViewById<TextView>(R.id.tvRol)
        val tvGrado  = view.findViewById<TextView>(R.id.tvGrado)

        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "No hay sesión iniciada", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val nombre   = doc.getString("nombre") ?: ""
                val apellido = doc.getString("apellido") ?: doc.getString("apellidos") ?: ""
                val rol = (doc.getString("role") ?: doc.getString("rol") ?: "—")
                val gradoRaw = doc.get("grado")       // puede venir "1" o 1
                val grado = gradoRaw?.toString() ?: "—"

                tvNombre.text = if (nombre.isBlank() && apellido.isBlank()) "—"
                else "$nombre $apellido"
                tvRol.text    = rol.replaceFirstChar { it.uppercase() }
                tvGrado.text  = grado
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
