package com.example.app_iesmdb

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CredentialsFragment : Fragment() {

    private lateinit var tvNombreDirector: TextView
    private lateinit var tvRolDirector: TextView

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_credentials, container, false)

        tvNombreDirector = view.findViewById(R.id.tvNombreDirector)
        tvRolDirector = view.findViewById(R.id.tvRolDirector)

        cargarDatosDirector()

        return view
    }

    private fun cargarDatosDirector() {
        val user = auth.currentUser ?: return

        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nombre = document.getString("nombre") ?: user.email ?: "Sin nombre"
                    val rol = document.getString("rol") ?: "Director"

                    tvNombreDirector.text = nombre
                    tvRolDirector.text = rol
                }
            }
            .addOnFailureListener {
                // Si falla, al menos mostramos algo por defecto
                tvNombreDirector.text = user.email ?: "Usuario sin nombre"
                tvRolDirector.text = "Director"
            }
    }
}
