package com.example.app_iesmdb

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import org.json.JSONObject
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class AuxiliarEscaneoQR : AppCompatActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var txtResultado: TextView
    private lateinit var btnPrueba: Button
    private lateinit var alertDialog : AlertDialog

    private val secretKey = "1234567890123456"
    private val CAMERA_REQUEST_CODE = 100


    private var lastScannedData: String? = null

    private val SCAN_COOLDOWN_MS = 1500 // evita múltiples lecturas del mismo QR en 2 segundos

    // 🧩 Variables para control de duplicados
    private var isDialogShowing = false
    private var lastScanTime = 0L
    private var lastScannedCode = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_auxiliar_escaneo_qr)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        barcodeView = findViewById(R.id.barcodeScannerView)
        txtResultado = findViewById(R.id.txtResultado)
//        btnPrueba = findViewById(R.id.btnPrueba)
//
//        btnPrueba.setOnClickListener {
//            val intent = Intent(this, activity_generate_qr::class.java)
//            startActivity(intent)
//        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_REQUEST_CODE
            )
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

            val currentTime = System.currentTimeMillis()

//            // 🚫 Evita múltiples lecturas del mismo QR en poco tiempo
//            if (qrData == lastScannedData && (currentTime - lastScanTime) < SCAN_COOLDOWN_MS) {
//                return@BarcodeCallback
//            }

            // 🔹 Evitar lecturas duplicadas en menos de 1.5s
            if (qrData == lastScannedCode && currentTime - lastScanTime < 1500) {
                Log.d("SCAN", "Lectura ignorada (repetida)")
                return@let
            }

            lastScannedCode = qrData
            lastScanTime = currentTime

            lastScannedData = qrData
            lastScanTime = currentTime

            try {
                val decrypted = decryptAES(qrData, secretKey)
                val jsonObject = JSONObject(decrypted)

                val id = jsonObject.getString("id")
//                val nombre = jsonObject.getString("nombre")
                val grado = jsonObject.getString("grado").trim()

                //Validar si estudiante existe o no
                Log.d("FIRESTORE", "INGRESO A LEER")
                getStudentFireStore(id,grado) { exito ->
                        if (exito) {
                            // ⚡ Mostrar modal al leer el QR
                            runOnUiThread {
                                barcodeView.pause() // 👈 Pausar antes de abrir modal
                                Log.d("MODAL", "Mostrando modal nuevo")
                                mostrarModal(id, grado)
                            }

                        }else{
                            runOnUiThread {
                                Toast.makeText(this, "⚠️ Estudiante no encontrado o datos inválidos", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "⚠️ QR inválido o no encriptado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun mostrarModal(id: String, grado: String) {

        // Evitar que se abra más de un modal a la vez
        if (isDialogShowing) {
            Log.d("MODAL", "Ya hay un modal mostrándose, no se crea otro")
            return
        }
        isDialogShowing = true

        Log.d("MODAL", "Mostrando modal nuevo")

        val dialogView = layoutInflater.inflate(R.layout.dialog_asistencia, null)
        val txtTitulo = dialogView.findViewById<TextView>(R.id.txtTitulo)
        val txtCodigo = dialogView.findViewById<TextView>(R.id.txtCodigo)
        val btnAsistio = dialogView.findViewById<Button>(R.id.btnAsistio)
        val btnTarde = dialogView.findViewById<Button>(R.id.btnTarde)

        txtCodigo.text = "$id"

        alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // 🔹 Escucha cuando el modal se cierra (por cualquier razón)
        alertDialog.setOnDismissListener {
            isDialogShowing = false
            Log.d("MODAL", "Modal cerrado correctamente")
        }

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()

        val window = alertDialog.window
        val params = window?.attributes
        params?.width = (resources.displayMetrics.widthPixels * 0.8).toInt() // 80% del ancho
        params?.height = (resources.displayMetrics.heightPixels * 0.3).toInt() // 35% de la altura total
        window?.attributes = params

        // 🔹 Botones
        btnAsistio.setOnClickListener {
            btnAsistio.isEnabled = false
            btnTarde.isEnabled = false
            alertDialog.dismiss()


            registerAttendaceFireStore(id,grado,"PUNTUAL") {exito ->
                runOnUiThread {
                    if(exito){
                        Toast.makeText(this, "Asistencia registrada como puntual ✅", Toast.LENGTH_SHORT).show()
                        txtResultado.text = "Asistencia confirmada: $id ($grado)"
                    }else{
                        Toast.makeText(this, "⚠️ No se pudo registrar la asistencia", Toast.LENGTH_SHORT).show()
                    }
                    barcodeView.resume()
                }
            }
        }

        btnTarde.setOnClickListener {

            btnAsistio.isEnabled = false
            btnTarde.isEnabled = false

            alertDialog.dismiss() // Cierra el modal inmediatamente

            registerAttendaceFireStore(id,grado,"TARDE"){ exito ->
                runOnUiThread {
                    if(exito){
                        Toast.makeText(this, "Asistencia registrada como tarde⏰", Toast.LENGTH_SHORT).show()
                        txtResultado.text = "Asistencia registrada como tarde⏰"
                    }else{
                        Toast.makeText(this, "⚠️ No se pudo registrar la asistencia", Toast.LENGTH_SHORT).show()
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
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

    override fun onResume() {
        super.onResume()
        barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    private fun getStudentFireStore(id:String,grade:String, callback: (Boolean) -> Unit )  {

        val db = FirebaseFirestore.getInstance()

        db.collection("estudiantes")
            .document(id)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if(documentSnapshot.exists()){
                    val nombre = documentSnapshot.getString("nombres")
                    val grado = documentSnapshot.getString("grado")
                    Log.d("FIRESTORE", "Estudiante: $nombre - Grado: $grado")
                    if(grado==grade){
                        callback(true)
                    }else{
                        callback(false)
                    }

                }else{
                    Log.d("FIRESTORE", "No existe estudiante con ese código")
                    callback(false)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FIRESTORE", "Error al leer documento", exception)
                callback(false)
            }


        //Esta forma es como poner un where coincidencia and coidencia ,
        //pero firebase me va pedir crear un indice compuesto si lo uso mucho
//        db.collection("estudiantes")
//            .whereEqualTo("id",id)
//            .whereEqualTo("grado",grade)
//            .get() //dependiendo a la coincidencias que obtenga , se contabilizaran las lecturas
//            .addOnSuccessListener { query ->
//                if(!query.isEmpty){
//                    val estudiante = query.documents[0]
//                    response=true
//                    Log.d("FireStore","Estudiante encontrado ${estudiante.getString("nombres")}")
//                }else{
//                    Log.d("FireStore","No se encontro estudiante")
//                }
//            }


    }

    private fun registerAttendaceFireStore(id:String,grado:String,estado:String, callback:(Boolean) -> Unit ) {

        val attendaceMap = hashMapOf(
            "id_estudiante" to id,
            "grado" to grado,
            "estado" to estado,
            "fecha" to Timestamp.now()
        )

        val db = FirebaseFirestore.getInstance()

        //con el método add genera un id automatico en la colección
        db.collection("asistencias_globales")
            .add(attendaceMap)
            .addOnSuccessListener { documentReference ->
                Log.d("FIRESTORE", "Asistencia registrada correctamente con ID: ${documentReference.id}")
                callback(true)
            }
            .addOnFailureListener { exception ->
                Log.e("FIRESTORE", "Error al registrar asistencia: ${exception.message}")
                callback(false)
            }
    }
}