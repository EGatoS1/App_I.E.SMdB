package com.example.app_iesmdb

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class UserFormDialog : DialogFragment() {

    // Si es null -> modo "nuevo". Si no -> modo "editar"
    var userToEdit: UserAccount? = null

    // Callback para devolver el resultado al fragment
    var onSubmit: ((Result) -> Unit)? = null

    data class Result(
        val isNew: Boolean,
        val username: String,
        val nombre: String,
        val apellido: String,
        val role: String,
        val grado: String?,
        val password: String?     // solo se usa al CREAR
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_user_form, null)

        // Views
        val tvTitle = view.findViewById<TextView>(R.id.tvTitleUserForm)
        val layoutGrade = view.findViewById<TextInputLayout>(R.id.layoutGrade)
        val layoutPassword = view.findViewById<TextInputLayout>(R.id.layoutPassword)

        val spinnerRole = view.findViewById<AutoCompleteTextView>(R.id.spinnerRole)
        val spinnerGrade = view.findViewById<AutoCompleteTextView>(R.id.spinnerGrade)
        val etName = view.findViewById<TextInputEditText>(R.id.etName)
        val etLastName = view.findViewById<TextInputEditText>(R.id.etLastName)
        val etUsername = view.findViewById<TextInputEditText>(R.id.etUsername)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)

        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSave)

        // ====== SPINNER ROL ======
        val rolesAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            UserConstants.ROLES_LIST
        )
        spinnerRole.setAdapter(rolesAdapter)

        // ====== SPINNER GRADO ======
        val gradesAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            UserConstants.GRADES_LIST
        )
        spinnerGrade.setAdapter(gradesAdapter)

        // Mostrar / ocultar grado según rol
        fun updateGradeVisibility(role: String?) {
            if (role == UserConstants.ROLE_TUTOR) {
                layoutGrade.visibility = TextInputLayout.VISIBLE
            } else {
                layoutGrade.visibility = TextInputLayout.GONE
                spinnerGrade.setText("", false)
            }
        }

        spinnerRole.setOnItemClickListener { parent, _, position, _ ->
            val selectedRole = parent.getItemAtPosition(position)?.toString()
            updateGradeVisibility(selectedRole)
        }

        // ====== MODO EDITAR O NUEVO ======
        val user = userToEdit
        if (user == null) {
            // Nuevo usuario
            tvTitle.text = "Nuevo usuario"
            layoutPassword.visibility = TextInputLayout.VISIBLE
            etUsername.isEnabled = true
        } else {
            // Editar usuario
            tvTitle.text = "Editar usuario"
            etName.setText(user.nombre)
            etLastName.setText(user.apellido)
            etUsername.setText(user.username)
            etUsername.isEnabled = false   // NO cambiamos username (email)
            spinnerRole.setText(user.role, false)

            if (user.role == UserConstants.ROLE_TUTOR && user.grado != null) {
                layoutGrade.visibility = TextInputLayout.VISIBLE
                spinnerGrade.setText(user.grado, false)
            } else {
                layoutGrade.visibility = TextInputLayout.GONE
            }

            layoutPassword.visibility = TextInputLayout.GONE   // no editamos contraseña aquí
        }

        // ====== BOTONES ======

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnSave.setOnClickListener {
            val role = spinnerRole.text.toString().trim()
            val grade = spinnerGrade.text.toString().trim().ifEmpty { null }
            val nombre = etName.text.toString().trim()
            val apellido = etLastName.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val password = etPassword.text?.toString()?.trim()

            // Validaciones simples
            if (role.isEmpty()) {
                spinnerRole.error = "Selecciona un rol"
                return@setOnClickListener
            }
            if (nombre.isEmpty()) {
                etName.error = "Ingresa un nombre"
                return@setOnClickListener
            }
            if (username.isEmpty()) {
                etUsername.error = "Ingresa un usuario"
                return@setOnClickListener
            }
            if (user == null && password.isNullOrEmpty()) {
                etPassword.error = "Ingresa una contraseña"
                return@setOnClickListener
            }
            if (role == UserConstants.ROLE_TUTOR && grade.isNullOrEmpty()) {
                spinnerGrade.error = "Selecciona un grado"
                return@setOnClickListener
            }

            val result = Result(
                isNew = (user == null),
                username = username,
                nombre = nombre,
                apellido = apellido,
                role = role,
                grado = grade,
                password = if (user == null) password else null
            )

            if (onSubmit == null) {
                // Por si acaso, para no quedarnos sin feedback
                Toast.makeText(requireContext(), "Sin listener de guardado", Toast.LENGTH_SHORT).show()
            } else {
                onSubmit?.invoke(result)
            }

            dismiss()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .create()
    }
}
