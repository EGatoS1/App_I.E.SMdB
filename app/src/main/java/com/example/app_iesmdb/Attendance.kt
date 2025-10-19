package com.example.app_iesmdb.attendance

data class Attendance(
    val date: String,
    val studentCode: String,
    val status: Status
)

enum class Status { PRESENTE, AUSENTE, TARDANZA }
