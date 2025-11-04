package com.example.app_iesmdb

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class UsersFragment : Fragment(R.layout.fragment_users) {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var adapter: UsersAdapter
    private lateinit var rvUsers: RecyclerView
    private lateinit var fabAdd: FloatingActionButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar: flecha vuelve atrás en el stack
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbarUsers)
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        rvUsers = view.findViewById(R.id.rvUsers)
        fabAdd = view.findViewById(R.id.fabAddUser)

        adapter = UsersAdapter(
            onEdit = { user ->
                // Más adelante abriremos el formulario de edición
                Toast.makeText(requireContext(), "Editar: ${user.fullName}", Toast.LENGTH_SHORT).show()
            },
            onDelete = { user ->
                // Más adelante haremos el diálogo + delete real
                Toast.makeText(requireContext(), "Eliminar: ${user.fullName}", Toast.LENGTH_SHORT).show()
            }
        )

        rvUsers.layoutManager = LinearLayoutManager(requireContext())
        rvUsers.adapter = adapter

        // Carga inicial de usuarios
        loadUsers()

        // Más adelante: abrir formulario de nuevo usuario
        fabAdd.setOnClickListener {
            Toast.makeText(requireContext(), "Nuevo usuario (formulario)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUsers() {
        db.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { doc ->
                    UserAccount(
                        uid = doc.id,
                        nombre = doc.getString("nombre") ?: "",
                        apellido = doc.getString("apellido") ?: "",
                        username = doc.getString("username") ?: "",
                        role = doc.getString("role") ?: "",
                        grado = doc.getString("grado")
                    )
                }.sortedBy { it.fullName.lowercase() }

                adapter.submitList(list)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error cargando usuarios: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}
