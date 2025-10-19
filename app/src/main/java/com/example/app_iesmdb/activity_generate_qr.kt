package com.example.app_iesmdb

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import org.json.JSONObject
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class activity_generate_qr : AppCompatActivity() {

    private val secretKey = "1234567890123456" // 16 caracteres
    lateinit var tvResultado : TextView;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_generate_qr)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etId = findViewById<EditText>(R.id.etId)
        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etApellidos = findViewById<EditText>(R.id.etApellidos)
        val etGrado = findViewById<EditText>(R.id.etGrado)
        val btnGenerar = findViewById<Button>(R.id.btnGenerar)
        val imgQr = findViewById<ImageView>(R.id.imgQr)
        val btnEscanear = findViewById<Button>(R.id.btnEscanear)
         tvResultado = findViewById<TextView>(R.id.tvResultado)


        val db = FirebaseFirestore.getInstance()
        db.collection("estudiantes").document("N00281581")

        btnGenerar.setOnClickListener {
            val id = etId.text.toString().trim()
            val nombre = etNombre.text.toString().trim()
            val grado = etGrado.text.toString().trim()
            val apellidos = etApellidos.text.toString().trim()

            if (id.isEmpty() || nombre.isEmpty() || grado.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {

                // Convertir los datos a JSON
                val json = JSONObject().apply {
                    put("id", id)
                    put("grado", grado)
//                    put("apellidos", grado)
//                    put("nombres", nombre)
                }.toString()

                //Creamos hashMap , sera el payload para el firstore
                val estudianteMap = hashMapOf(
                    "id" to id,
                    "nombres" to nombre,
                    "apellidos" to apellidos,
                    "grado" to grado
                    //"fechaRegistro" to System.currentTimeMillis()
                )

                // Encriptar el JSON
                val encryptedText = encryptAES(json, secretKey)

                // Generar el QR
                val qrBitmap = generateQRCode(encryptedText)
                imgQr.setImageBitmap(qrBitmap)


                addStudentFireStore(id,estudianteMap)

            }catch(e: Exception) {
                Toast.makeText(this,"Ocurrio un error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }

        btnEscanear.setOnClickListener {
            val intent = Intent(this, AuxiliarMenu::class.java)
            startActivity(intent)
        }
    }


    private fun addStudentFireStore(id:String,estudianteMap: HashMap<String, String>){
        //Guardado en FireStore
        val db = FirebaseFirestore.getInstance()
        db.collection("estudiantes")
            .document(id)
            .set(estudianteMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Estudiante registrado correctamente", Toast.LENGTH_SHORT).show()
                tvResultado.setText("Estudiante registrado correctamente")
                Log.e("FIRESTORE","Estudiante registrado correctamente")
            }
            .addOnFailureListener { e->
                Toast.makeText(this, "Error al registrar: ${e.message}", Toast.LENGTH_SHORT).show()
                tvResultado.setText("Error al registrar: ${e.message}")
                Log.e("FIRESTORE","Error al registrar: ${e.message}")
            }
    }

    private fun generateQRCode(data: String): Bitmap? {
        return try {
            val barcodeEncoder = BarcodeEncoder()
            barcodeEncoder.encodeBitmap(data, BarcodeFormat.QR_CODE, 400, 400)
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }
    private fun encryptAES(text: String, secretKey: String): String {
        val keySpec = SecretKeySpec(secretKey.toByteArray(), "AES")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        val encryptedBytes = cipher.doFinal(text.toByteArray())
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
    }
}