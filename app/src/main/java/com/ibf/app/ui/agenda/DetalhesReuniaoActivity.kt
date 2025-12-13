package com.ibf.app.ui.agenda

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.ibf.app.R
import com.ibf.app.data.models.Reuniao
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

class DetalhesReuniaoActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var reuniaoId: String? = null
    private var currentReuniao: Reuniao? = null

    // Audio
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false

    // UI
    private lateinit var textStatus: TextView
    private lateinit var btnRecord: MaterialButton
    private lateinit var btnStop: MaterialButton
    private lateinit var imagePreview: ImageView

    // Permissions
    private val RECORD_AUDIO_REQUEST_CODE = 101

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                imagePreview.setImageBitmap(imageBitmap)
                uploadPhoto(imageBitmap)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhes_reuniao)

        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        reuniaoId = intent.getStringExtra("REUNIAO_ID")

        if (reuniaoId == null) {
            finish()
            return
        }

        initViews()
        carregarDetalhes()
    }

    private fun initViews() {
        textStatus = findViewById(R.id.text_audio_status)
        btnRecord = findViewById(R.id.btn_record)
        btnStop = findViewById(R.id.btn_stop)
        imagePreview = findViewById(R.id.image_preview)

        btnRecord.setOnClickListener {
            if (checkPermissions()) {
                startRecording()
            } else {
                requestPermissions()
            }
        }
        
        btnStop.setOnClickListener { stopRecording() }

        findViewById<MaterialButton>(R.id.btn_photo).setOnClickListener {
            dispatchTakePictureIntent()
        }
    }

    private fun carregarDetalhes() {
        reuniaoId?.let { id ->
            firestore.collection("reunioes").document(id).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val r = document.toObject(Reuniao::class.java)
                        r?.id = document.id
                        currentReuniao = r
                        updateUI(r)
                    }
                }
        }
    }

    private fun updateUI(r: Reuniao?) {
        if (r == null) return
        findViewById<TextView>(R.id.text_membro).text = "Membro: ${r.membroNome}"
        findViewById<TextView>(R.id.text_local).text = "Local: ${r.local}"
        findViewById<TextView>(R.id.text_obs).text = "Observações: ${r.observacoes}"
        
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
        findViewById<TextView>(R.id.text_data).text = "Data: " + (r.dataHora?.toDate()?.let { format.format(it) } ?: "")

        if (r.audioUrl.isNotEmpty()) {
            textStatus.text = "Áudio anexado (Upload concluído)"
        }
        
        // Load image if url exists (for now just placeholder or text update, glide/picasso recommended)
        // using native tools available or simplistic approach
    }

    // --- AUDIO LOGIC ---

    private fun startRecording() {
        audioFile = File(externalCacheDir?.absolutePath, "audiorecord_${System.currentTimeMillis()}.3gp")

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(audioFile?.absolutePath)
            
            try {
                prepare()
                start()
                isRecording = true
                textStatus.text = "Gravando..."
                btnRecord.isEnabled = false
                btnStop.isEnabled = true
            } catch (e: IOException) {
                Log.e("Audio", "prepare() failed")
            }
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        isRecording = false
        textStatus.text = "Gravação finalizada. Enviando..."
        btnRecord.isEnabled = true
        btnStop.isEnabled = false
        
        uploadAudio()
    }

    private fun uploadAudio() {
        val file = audioFile ?: return
        val uri = Uri.fromFile(file)
        val ref = storage.reference.child("reunioes/${reuniaoId}/audio.3gp")
        
        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUri ->
                    updateFirestore("audioUrl", downloadUri.toString())
                    textStatus.text = "Áudio salvo com sucesso!"
                }
            }
            .addOnFailureListener {
                textStatus.text = "Falha no upload do áudio."
                Toast.makeText(this, "Erro upload: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // --- PHOTO LOGIC ---

    private fun dispatchTakePictureIntent() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            cameraLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Câmera indisponível", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadPhoto(bitmap: Bitmap) {
        val ref = storage.reference.child("reunioes/${reuniaoId}/foto.jpg")
        val baos = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val data = baos.toByteArray()

        Toast.makeText(this, "Enviando foto...", Toast.LENGTH_SHORT).show()
        
        ref.putBytes(data)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUri ->
                    updateFirestore("fotoUrl", downloadUri.toString())
                    Toast.makeText(this, "Foto salva!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao enviar foto.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateFirestore(field: String, url: String) {
        reuniaoId?.let { id ->
            firestore.collection("reunioes").document(id)
                .update(field, url)
        }
    }

    // --- PERMISSIONS ---

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE), RECORD_AUDIO_REQUEST_CODE)
    }
}
