package com.example.app_iesmdb

/**
 * Constantes relacionadas a usuarios y autenticación.
 */
object UserConstants {

    // Dominio que se usará para construir el email:
    // email = "$username$EMAIL_DOMAIN"
    // Cámbialo por el que realmente uses en tu proyecto.
    const val EMAIL_DOMAIN = "@demo.local"

    // Roles permitidos en la colección "users"
    const val ROLE_ADMIN = "administrativo"
    const val ROLE_TUTOR = "tutor"
    const val ROLE_AUX = "auxiliar"

    // Lista de roles para spinners, etc.
    val ROLES_LIST = listOf(ROLE_ADMIN, ROLE_TUTOR, ROLE_AUX)

    // Grados posibles (ajusta si tu cole tiene otros)
    val GRADES_LIST = listOf("1", "2", "3", "4", "5", "6")
}
