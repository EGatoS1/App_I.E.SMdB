package com.example.app_iesmdb

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_auxiliar_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Tu Card puede ser MaterialCardView; usa el id raíz de la tarjeta de escaneo
        cardScan = view.findViewById(R.id.cardScan)
        tvTotal = view.findViewById(R.id.txtTotalScans)

        // Navegación al fragment de escaneo
        cardScan.setOnClickListener {
            findNavController().navigate(R.id.action_auxiliarHome_to_scan)
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

    private fun attachTodayCounter() {
        // Rango de HOY en la zona local del dispositivo
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = Timestamp(cal.time)
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val end = Timestamp(cal.time)

        // Consulta: asistencias registradas HOY
        val query = db.collection("asistencias_globales")
            .whereGreaterThanOrEqualTo("fecha", start)
            .whereLessThan("fecha", end)

        // Escucha en tiempo real (si agregan/quitan docs, se actualiza solo)
        todayListener = query.addSnapshotListener { snap, _ ->
            val count = snap?.size() ?: 0
            tvTotal.text = NumberFormat.getIntegerInstance(Locale.getDefault()).format(count)
        }
    }
}
