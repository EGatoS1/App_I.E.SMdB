package com.example.app_iesmdb

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore

class AuxiliarMenu : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_auxiliar_menu)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val llControlAsistencia = findViewById<LinearLayout>(R.id.llControlAsistencia)

        llControlAsistencia.setOnClickListener {
            val miIntent = Intent(this, AuxiliarEscaneoQR::class.java)
            startActivity(miIntent)
        }

        insertarEstudiantesMasivo()
    }


    fun insertarEstudiantesMasivo() {
        val db = FirebaseFirestore.getInstance()

        val nombres = listOf(
            "Carlos", "María", "Luis", "Andrea", "José", "Daniela", "Javier", "Camila", "Pedro", "Lucía",
            "Diego", "Valeria", "Sofía", "Juan", "Gabriela", "Fernando", "Ana", "Sebastián", "Patricia", "Rodrigo",
            "Julio", "Karla", "Enrique", "Rosa", "Paolo", "Milagros", "Raúl", "Melissa", "Alberto", "Natalia",
            "Hugo", "Verónica", "Martín", "Estefanía", "Alex", "Carolina", "Ignacio", "Victoria", "Renzo", "Flor",
            "Gustavo", "Marina", "Bruno", "Paula", "Cristian", "Elsa", "Mauricio", "Carmen", "Oscar", "Diana",
            "Ernesto", "Nora", "Santiago", "Pamela", "Felipe", "Noelia", "Jorge", "Sandra", "Andrés", "Claudia",
            "Kevin", "Luz", "Ricardo", "Rocío", "Antonio", "Teresa", "Manuel", "Elena", "Adrián", "Bárbara",
            "Federico", "Liliana", "Pablo", "Martha", "Mario", "Lorena", "Rafael", "Silvia", "Gonzalo", "Susana",
            "Esteban", "Leticia", "Víctor", "Alicia", "Marco", "Cecilia", "Alan", "Yolanda", "Francisco", "Paty"
        )

        val apellidos = listOf(
            "Torres", "Pérez", "Ramírez", "Flores", "García", "Mendoza", "Salazar", "Vargas", "Huamán", "Rojas",
            "Castro", "Gutiérrez", "López", "Chávez", "Rivas", "Hidalgo", "Poma", "Quispe", "Ramos", "Velarde",
            "Acosta", "Delgado", "Cáceres", "Valdez", "Ortega", "Morales", "Zapata", "Reyes", "Campos", "Valverde",
            "Suárez", "Carrillo", "Lozano", "Mejía", "Tafur", "Cornejo", "Espinoza", "Nuñez", "León", "Villanueva",
            "Sánchez", "Benavides", "Sotomayor", "Alvarado", "Palomino", "Camacho", "Aguilar", "Farro", "Cardenas", "Montoya",
            "Ibáñez", "Guevara", "Quiroz", "Salinas", "Zamora", "Valencia", "Herrera", "Muñoz", "Galvez", "Tapia",
            "Espino", "Ruiz", "Cárcamo", "Bravo", "Romero", "Arce", "Villena", "Calderón", "Prieto", "Caballero",
            "Miranda", "Pizarro", "Becerra", "Bustamante", "Reynoso", "Linares", "Rengifo", "Montalvo", "Contreras", "Vallejo",
            "Meza", "Huerta", "Toribio", "Bautista", "Zúñiga", "Palacios", "Gonzales", "Córdova", "Castillo", "Fernández"
        )

        var contador = 1

        // 🔹 Generar 30 estudiantes por cada grado del 1 al 6
        for (grado in 1..6) {
            for (i in 1..30) {
                val id = "N" + contador.toString().padStart(8, '0')
                val nombre = "${nombres.random()} ${nombres.random()}"
                val apellido = "${apellidos.random()} ${apellidos.random()}"

                val estudiante = hashMapOf(
                    "id" to id,
                    "nombres" to nombre,
                    "apellidos" to apellido,
                    "grado" to grado
                )

                db.collection("estudiantes").document(id)
                    .set(estudiante)
                    .addOnSuccessListener {
                        Log.d("Firestore", "✅ Insertado: $id (grado $grado)")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "❌ Error al insertar $id", e)
                    }

                contador++
            }
        }
    }





}