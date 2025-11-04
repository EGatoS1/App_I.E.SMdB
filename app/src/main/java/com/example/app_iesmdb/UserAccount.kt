package com.example.app_iesmdb

/**
 * Representa un usuario de la colección "users" de Firestore.
 * El id del documento será el uid de Firebase Authentication.
 */
data class UserAccount(
    val uid: String = "",
    val nombre: String = "",
    val apellido: String = "",
    val username: String = "",
    val role: String = "",
    val grado: String? = null     // solo se usa si es tutor
) {
    val fullName: String
        get() = listOf(nombre, apellido)
            .filter { it.isNotBlank() }
            .joinToString(" ")
}
