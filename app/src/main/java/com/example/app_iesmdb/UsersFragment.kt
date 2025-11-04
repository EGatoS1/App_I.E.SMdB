package com.example.app_iesmdb

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UsersFragment : Fragment(R.layout.fragment_users) {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private lateinit var adapter: UsersAdapter
    private lateinit var rvUsers: RecyclerView
    private lateinit var fabAdd: FloatingActionButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar back
        view.findViewById<MaterialToolbar?>(R.id.toolbarUsers)?.apply {
            setNavigationOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        rvUsers = view.findViewById(R.id.rvUsers)
        fabAdd = view.findViewById(R.id.fabAddUser)

        rvUsers.layoutManager = LinearLayoutManager(requireContext())

        adapter = UsersAdapter(
            onEdit = { user -> openUserForm(user) },
            onDelete = { user -> confirmDeleteUser(user) }
        )
        rvUsers.adapter = adapter

        fabAdd.setOnClickListener {
            openUserForm(null)   // nuevo usuario
        }

        loadUsers()
    }

    // ======================= CARGA LISTA =======================

    private fun loadUsers() {
        db.collection("users")
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.mapNotNull { doc ->
                    val username = doc.getString("username") ?: return@mapNotNull null
                    val nombre = doc.getString("nombre") ?: ""
                    val apellido = doc.getString("apellido") ?: ""

                    // 👇 Preferimos 'rol', pero aceptamos 'role' si existe
                    val role = doc.getString("rol")
                        ?: doc.getString("role")
                        ?: ""

                    val grado = doc.getString("grado")

                    UserAccount(
                        uid = doc.id,
                        username = username,
                        nombre = nombre,
                        apellido = apellido,
                        role = role,
                        grado = grado
                    )
                }.sortedBy { it.fullName.lowercase() }

                adapter.submitList(list)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error cargando usuarios: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ======================= FORMULARIO =======================

    private fun openUserForm(user: UserAccount?) {
        val dialog = UserFormDialog()
        dialog.userToEdit = user

        dialog.onSubmit = { result ->
            if (result.isNew) {
                // CREAR
                val userForCreate = UserAccount(
                    uid = "",
                    username = result.username,
                    nombre = result.nombre,
                    apellido = result.apellido,
                    role = result.role,
                    grado = result.grado
                )
                createUserInAuthAndFirestore(userForCreate, result.password!!)
            } else {
                // EDITAR: sólo Firestore
                val current = user
                if (current != null) {
                    val updated = current.copy(
                        nombre = result.nombre,
                        apellido = result.apellido,
                        role = result.role,
                        grado = result.grado
                    )
                    updateUserInFirestore(updated)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Error: usuario nulo al editar",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        dialog.show(parentFragmentManager, "UserFormDialog")
    }

    // ======================= CREAR =======================

    private fun createUserInAuthAndFirestore(user: UserAccount, password: String) {
        val email = "${user.username}${UserConstants.EMAIL_DOMAIN}"

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: return@addOnSuccessListener

                val data = hashMapOf(
                    "username" to user.username,
                    "nombre" to user.nombre,
                    "apellido" to user.apellido,
                    // 👇 Guardamos SIEMPRE en 'rol'
                    "rol" to user.role,
                    "grado" to (user.grado ?: "")
                )

                db.collection("users").document(uid)
                    .set(data)
                    .addOnSuccessListener {
                        Toast.makeText(
                            requireContext(),
                            "Usuario creado",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadUsers()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            requireContext(),
                            "Error guardando en Firestore: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error creando en Auth: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ======================= EDITAR (Firestore) =======================

    private fun updateUserInFirestore(user: UserAccount) {
        if (user.uid.isBlank()) {
            Toast.makeText(
                requireContext(),
                "UID vacío, no se puede actualizar",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val data = hashMapOf(
            "username" to user.username,
            "nombre" to user.nombre,
            "apellido" to user.apellido,
            // 👇 De nuevo, sólo 'rol'
            "rol" to user.role,
            "grado" to (user.grado ?: "")
        )

        db.collection("users").document(user.uid)
            .update(data as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Usuario actualizado",
                    Toast.LENGTH_SHORT
                ).show()
                loadUsers()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error actualizando: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ======================= ELIMINAR (Firestore) =======================

    private fun confirmDeleteUser(user: UserAccount) {
        deleteUserInFirestore(user)
    }

    private fun deleteUserInFirestore(user: UserAccount) {
        if (user.uid.isBlank()) {
            Toast.makeText(
                requireContext(),
                "UID vacío, no se puede eliminar",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        db.collection("users").document(user.uid)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Usuario eliminado (Firestore)",
                    Toast.LENGTH_SHORT
                ).show()
                loadUsers()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error eliminando: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
