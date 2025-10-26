package com.example.app_iesmdb

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuxiliarCredentialsFragment : Fragment(R.layout.fragment_auxiliar_credentials) {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvNombre = view.findViewById<TextView>(R.id.tvNombreCompleto)
        val tvRol    = view.findViewById<TextView>(R.id.tvRol)

        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "No hay sesión iniciada", Toast.LENGTH_SHORT).show()
            tvNombre.text = "—"
            tvRol.text = "—"
            return
        }

        // MISMA COLECCIÓN Y CAMPOS QUE USA TEACHER
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val nombre   = doc.getString("nombre") ?: ""
                val apellido = doc.getString("apellido") ?: doc.getString("apellidos") ?: ""
                val rol      = (doc.getString("role") ?: doc.getString("rol") ?: "—")

                // Si no hay nombre/apellido en Firestore, usa displayName como respaldo
                val displayName = auth.currentUser?.displayName

                tvNombre.text = when {
                    nombre.isNotBlank() || apellido.isNotBlank() -> "$nombre $apellido".trim()
                    !displayName.isNullOrBlank() -> displayName
                    else -> "—"
                }
                tvRol.text = if (rol == "—") "Auxiliar" else rol.replaceFirstChar { it.uppercase() }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                tvNombre.text = auth.currentUser?.displayName ?: "—"
                tvRol.text = "Auxiliar"
            }
    }
}
