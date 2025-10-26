package com.example.app_iesmdb

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import org.json.JSONObject
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class AuxiliarScanFragment : Fragment() {

    // UI
    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var txtResultado: TextView

    // Firestore
    private val db by lazy { FirebaseFirestore.getInstance() }

    // Seguridad / escáner
    private val secretKey = "1234567890123456"         // misma llave que usó tu compañero
    private val CAMERA_REQUEST_CODE = 100
    private var isDialogShowing = false
    private var lastScanTime = 0L
    private var lastScannedCode = ""
    private val SCAN_COOLDOWN_MS = 1500L               // evita lecturas repetidas en < 1.5s

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_auxiliar_scan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        barcodeView = view.findViewById(R.id.barcodeScannerView)
        txtResultado = view.findViewById(R.id.txtResultado)

        // Permisos de cámara
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
        } else {
            iniciarEscaneo()
        }
    }

    private fun iniciarEscaneo() {
        barcodeView.decodeContinuous(callback)
        barcodeView.resume()
    }

    private val callback = BarcodeCallback { result: BarcodeResult? ->
        result?.text?.let { qrData ->
            val now = System.currentTimeMillis()
            if (qrData == lastScannedCode && (now - lastScanTime) < SCAN_COOLDOWN_MS) {
                // lectura repetida muy seguida → ignorar
                return@let
            }
            lastScannedCode = qrData
            lastScanTime = now

            try {
                val decrypted = decryptAES(qrData, secretKey)
                val json = JSONObject(decrypted)
                val id = json.getString("id")
                val grado = json.getString("grado").trim()

                // Validar que el estudiante exista y el grado coincida
                getStudentFireStore(id, grado) { ok ->
                    if (!isAdded) return@getStudentFireStore
                    if (ok) {
                        // Pausar cámara y mostrar modal
                        requireActivity().runOnUiThread {
                            barcodeView.pause()
                            mostrarModal(id, grado)
                        }
                    } else {
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireContext(),
                                "⚠️ Estudiante no encontrado o grado inválido",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(),
                        "⚠️ QR inválido o no encriptado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun mostrarModal(id: String, grado: String) {
        if (isDialogShowing) return
        isDialogShowing = true

        val dialogView = layoutInflater.inflate(R.layout.dialog_asistencia, null)
        val txtCodigo = dialogView.findViewById<TextView>(R.id.txtCodigo)
        txtCodigo.text = id

        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        alertDialog.setOnDismissListener { isDialogShowing = false }
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()

        // Ajuste opcional de tamaño (80% ancho, 30% alto)
        alertDialog.window?.let { w ->
            val p = w.attributes
            p.width = (resources.displayMetrics.widthPixels * 0.8f).toInt()
            p.height = (resources.displayMetrics.heightPixels * 0.3f).toInt()
            w.attributes = p
        }

        val btnAsistio = dialogView.findViewById<View>(R.id.btnAsistio)
        val btnTarde   = dialogView.findViewById<View>(R.id.btnTarde)

        btnAsistio.setOnClickListener {
            btnAsistio.isEnabled = false; btnTarde.isEnabled = false
            alertDialog.dismiss()
            registerAttendanceFirestore(id, grado, "PUNTUAL") { exito ->
                if (!isAdded) return@registerAttendanceFirestore
                requireActivity().runOnUiThread {
                    if (exito) {
                        Toast.makeText(requireContext(),
                            "Asistencia registrada como puntual ✅",
                            Toast.LENGTH_SHORT).show()
                        txtResultado.text = "Asistencia confirmada: $id ($grado)"
                    } else {
                        Toast.makeText(requireContext(),
                            "⚠️ No se pudo registrar la asistencia",
                            Toast.LENGTH_SHORT).show()
                    }
                    barcodeView.resume()
                }
            }
        }

        btnTarde.setOnClickListener {
            btnAsistio.isEnabled = false; btnTarde.isEnabled = false
            alertDialog.dismiss()
            registerAttendanceFirestore(id, grado, "TARDE") { exito ->
                if (!isAdded) return@registerAttendanceFirestore
                requireActivity().runOnUiThread {
                    if (exito) {
                        Toast.makeText(requireContext(),
                            "Asistencia registrada como tarde ⏰",
                            Toast.LENGTH_SHORT).show()
                        txtResultado.text = "Asistencia registrada como tarde ⏰"
                    } else {
                        Toast.makeText(requireContext(),
                            "⚠️ No se pudo registrar la asistencia",
                            Toast.LENGTH_SHORT).show()
                    }
                    barcodeView.resume()
                }
            }
        }
    }

    private fun decryptAES(encrypted: String, secretKey: String): String {
        val keySpec = SecretKeySpec(secretKey.toByteArray(), "AES")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, keySpec)
        val decodedBytes = Base64.decode(encrypted, Base64.DEFAULT)
        val decryptedBytes = cipher.doFinal(decodedBytes)
        return String(decryptedBytes)
    }

    private fun getStudentFireStore(id: String, grade: String, callback: (Boolean) -> Unit) {
        db.collection("estudiantes").document(id).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val gradoDoc = doc.getString("grado")?.trim()
                    callback(gradoDoc == grade)
                } else callback(false)
            }
            .addOnFailureListener { e ->
                Log.e("FIRESTORE", "Error obteniendo estudiante: ${e.message}")
                callback(false)
            }
    }

    private fun registerAttendanceFirestore(
        id: String,
        grado: String,
        estado: String,
        callback: (Boolean) -> Unit
    ) {
        val attendaceMap = hashMapOf(
            "id_estudiante" to id,
            "grado" to grado,
            "estado" to estado,
            "fecha" to Timestamp.now()
        )
        db.collection("asistencias_globales")
            .add(attendaceMap)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { e ->
                Log.e("FIRESTORE", "Error registrando asistencia: ${e.message}")
                callback(false)
            }
    }

    override fun onResume() {
        super.onResume()
        if (::barcodeView.isInitialized) barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        if (::barcodeView.isInitialized) barcodeView.pause()
    }

    // Permisos
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            iniciarEscaneo()
        } else {
            txtResultado.text = "❌ Permiso de cámara denegado"
        }
    }
}
