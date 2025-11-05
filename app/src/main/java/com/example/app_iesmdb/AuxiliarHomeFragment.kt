package com.example.app_iesmdb

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class AuxiliarHomeFragment : Fragment() {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private var todayListener: ListenerRegistration? = null

    private lateinit var cardScan: CardView
    private lateinit var tvTotal: TextView
    private lateinit var cardMarkAbsences: MaterialCardView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_auxiliar_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cardScan = view.findViewById(R.id.cardScan)
        tvTotal = view.findViewById(R.id.txtTotalScans)
        cardMarkAbsences = view.findViewById(R.id.cardMarkAbsences)

        // Navegación al fragment de escaneo
        cardScan.setOnClickListener {
            findNavController().navigate(R.id.action_auxiliarHome_to_scan)
        }

        // Card para marcar faltas del día
        cardMarkAbsences.setOnClickListener {
            confirmarMarcadoFaltas()
        }

        // Muestra 0 mientras carga
        tvTotal.text = "0"
    }

    override fun onStart() {
        super.onStart()
        attachTodayCounter()
    }

    override fun onStop() {
        super.onStop()
        todayListener?.remove()
        todayListener = null
    }

    // ================== CONTADOR DE HOY ==================

    private fun attachTodayCounter() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = Timestamp(cal.time)
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val end = Timestamp(cal.time)

        val query = db.collection("asistencias_globales")
            .whereGreaterThanOrEqualTo("fecha", start)
            .whereLessThan("fecha", end)

        todayListener = query.addSnapshotListener { snap, _ ->
            val count = snap?.size() ?: 0
            tvTotal.text =
                NumberFormat.getIntegerInstance(Locale.getDefault()).format(count)
        }
    }

    // ================== MARCAR FALTAS DEL DÍA ==================

    private fun confirmarMarcadoFaltas() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Marcar faltas del día")
            .setMessage(
                "Se marcará como FALTA a todos los estudiantes que hoy " +
                        "no tengan asistencia registrada. ¿Deseas continuar?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Sí, marcar") { _, _ ->
                marcarFaltasDeHoyParaPendientes()
            }
            .show()
    }

    private fun marcarFaltasDeHoyParaPendientes() {
        // Rango de hoy
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val inicioHoy = Timestamp(cal.time)
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val finHoy = Timestamp(cal.time)

        // 1) Obtener todos los estudiantes
        db.collection("estudiantes")
            .get()
            .addOnSuccessListener { snapEstudiantes ->
                val todosEstudiantes = snapEstudiantes.documents.mapNotNull { doc ->
                    val id = doc.id
                    val grado = doc.get("grado")?.toString() ?: return@mapNotNull null
                    id to grado
                }

                // 2) Obtener asistencias de HOY
                db.collection("asistencias_globales")
                    .whereGreaterThanOrEqualTo("fecha", inicioHoy)
                    .whereLessThan("fecha", finHoy)
                    .get()
                    .addOnSuccessListener { snapAsistencias ->
                        val yaMarcados = snapAsistencias.documents
                            .mapNotNull { it.getString("id_estudiante") }
                            .toSet()

                        val pendientes = todosEstudiantes.filter { (id, _) ->
                            !yaMarcados.contains(id)
                        }

                        if (pendientes.isEmpty()) {
                            Toast.makeText(
                                requireContext(),
                                "Hoy ya tienen registro todos los estudiantes.",
                                Toast.LENGTH_LONG
                            ).show()
                            return@addOnSuccessListener
                        }

                        val batch = db.batch()
                        val ahora = Timestamp.now()

                        pendientes.forEach { (idEst, grado) ->
                            val ref = db.collection("asistencias_globales").document()
                            val data = hashMapOf(
                                "id_estudiante" to idEst,
                                "grado" to grado,
                                "estado" to "FALTA",
                                "fecha" to ahora
                            )
                            batch.set(ref, data)
                        }

                        batch.commit()
                            .addOnSuccessListener {
                                Toast.makeText(
                                    requireContext(),
                                    "Se marcaron faltas para ${pendientes.size} estudiante(s).",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    requireContext(),
                                    "Error marcando faltas: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            requireContext(),
                            "Error leyendo asistencias: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error leyendo estudiantes: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
