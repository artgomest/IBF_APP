package com.ibf.app.ui.agenda

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.ibf.app.R
import com.ibf.app.data.models.Reuniao
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.TimeZone
import java.util.Locale
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.Timestamp
import java.io.File
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide

class DetalhesReuniaoActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var reuniaoId: String? = null
    private var currentReuniao: Reuniao? = null
    private var mediaPlayer: android.media.MediaPlayer? = null
    private var isPlaying = false

    private lateinit var imagePreview: ImageView
    private lateinit var btnPreencherRelatorio: MaterialButton
    private lateinit var textRelatorioOk: TextView
    private lateinit var textStatusBadge: TextView
    private lateinit var layoutAcoesAgenda: View
    private lateinit var btnRemarcar: MaterialButton
    private lateinit var btnCancelar: MaterialButton
    private lateinit var sectionAudioPlayer: View
    private lateinit var btnPlayAudio: MaterialButton

    private val calendar = Calendar.getInstance()

    private var photoFile: File? = null
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            photoFile?.let { file ->
                if (file.exists()) {
                    uploadPhotoFromFile(file)
                    // Preview the image
                    imagePreview.setImageURI(androidx.core.content.FileProvider.getUriForFile(this, "${packageName}.fileprovider", file))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhes_reuniao)

        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        reuniaoId = intent.getStringExtra("REUNIAO_ID")

        if (reuniaoId == null) { finish(); return }

        initViews()
    }

    override fun onResume() {
        super.onResume()
        carregarDetalhes()
    }

    private fun initViews() {
        imagePreview = findViewById(R.id.image_preview)
        btnPreencherRelatorio = findViewById(R.id.btn_preencher_relatorio)
        textRelatorioOk = findViewById(R.id.text_relatorio_ok)
        textStatusBadge = findViewById(R.id.text_status_badge)
        layoutAcoesAgenda = findViewById(R.id.layout_acoes_agenda)
        btnRemarcar = findViewById(R.id.btn_remarcar)
        btnCancelar = findViewById(R.id.btn_cancelar)
        sectionAudioPlayer = findViewById(R.id.section_audio_player)
        btnPlayAudio = findViewById(R.id.btn_play_audio)

        findViewById<ImageView>(R.id.button_back).setOnClickListener { finish() }

        btnCancelar.setOnClickListener { confirmarCancelamento() }
        btnRemarcar.setOnClickListener { showDatePicker() }
        btnPlayAudio.setOnClickListener { toggleAudio() }

        findViewById<MaterialButton>(R.id.btn_photo).setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val file = File(externalCacheDir, "discipulado_${reuniaoId}_${System.currentTimeMillis()}.jpg")
            photoFile = file
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            try { cameraLauncher.launch(intent) } catch (e: Exception) {
                Toast.makeText(this, "Câmera indisponível", Toast.LENGTH_SHORT).show()
            }
        }

        btnPreencherRelatorio.setOnClickListener {
            val intent = Intent(this, RelatorioReuniaoActivity::class.java)
            intent.putExtra("REUNIAO_ID", reuniaoId)
            startActivity(intent)
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
        val obs = if (r.observacoes.isNotEmpty()) r.observacoes else "—"
        findViewById<TextView>(R.id.text_obs).text = "Observações: $obs"

        val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
        val dataFormatada = r.dataHora?.toDate()?.let { format.format(it) } ?: "—"
        findViewById<TextView>(R.id.text_data).text = "Data: $dataFormatada"

        // Status badge with rounded background
        textStatusBadge.text = r.status
        val bgColorRes = when (r.status) {
            "Concluída" -> R.color.ibf_success
            "Cancelada" -> R.color.ibf_error
            else -> R.color.ibf_warning
        }
        val badgeDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16f * resources.displayMetrics.density
            setColor(ContextCompat.getColor(this@DetalhesReuniaoActivity, bgColorRes))
        }
        textStatusBadge.background = badgeDrawable

        // Show photo if exists
        if (r.fotoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(r.fotoUrl)
                .into(imagePreview)
        }

        // Show audio player if audioUrl exists
        if (!r.audioUrl.isNullOrEmpty()) {
            sectionAudioPlayer.visibility = View.VISIBLE
        } else {
            sectionAudioPlayer.visibility = View.GONE
        }

        // Show "Preencher Relatório" button only after meeting has passed AND photo is taken
        val dataReuniao = r.dataHora?.toDate()
        if (dataReuniao != null && dataReuniao.before(Date())) {
            if (r.relatorioPreenchido) {
                btnPreencherRelatorio.visibility = View.GONE
                textRelatorioOk.visibility = View.VISIBLE
            } else {
                btnPreencherRelatorio.visibility = View.VISIBLE
                btnPreencherRelatorio.isEnabled = true
                btnPreencherRelatorio.text = "Preencher Relatório"
                textRelatorioOk.visibility = View.GONE
            }
        } else {
            btnPreencherRelatorio.visibility = View.GONE
            textRelatorioOk.visibility = View.GONE
        }

        // Show/hide Reschedule/Cancel actions (only for Agendada meetings)
        if (r.status == "Agendada") {
            layoutAcoesAgenda.visibility = View.VISIBLE
        } else {
            layoutAcoesAgenda.visibility = View.GONE
        }
    }

    private fun confirmarCancelamento() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Cancelar Discipulado")
            .setMessage("Tem certeza que deseja cancelar este agendamento?")
            .setPositiveButton("Sim") { _, _ ->
                reuniaoId?.let { id ->
                    firestore.collection("reunioes").document(id)
                        .update("status", "Cancelada")
                        .addOnSuccessListener {
                            Toast.makeText(this, "Agendamento cancelado", Toast.LENGTH_SHORT).show()
                            carregarDetalhes()
                        }
                }
            }
            .setNegativeButton("Não", null)
            .show()
    }

    private fun showDatePicker() {
        val datePicker = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
            .setTitleText("Selecione a Nova Data")
            .setSelection(calendar.timeInMillis)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            utcCalendar.timeInMillis = selection
            
            // Atribuição atômica para evitar bugs de fuso/mês
            calendar.set(
                utcCalendar.get(Calendar.YEAR),
                utcCalendar.get(Calendar.MONTH),
                utcCalendar.get(Calendar.DAY_OF_MONTH)
            )
            
            showTimePicker()
        }
        datePicker.show(supportFragmentManager, "DATE_PICKER")
    }

    private fun showTimePicker() {
        val timePicker = com.google.android.material.timepicker.MaterialTimePicker.Builder()
            .setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_24H)
            .setHour(calendar.get(Calendar.HOUR_OF_DAY))
            .setMinute(calendar.get(Calendar.MINUTE))
            .setTitleText("Selecione a Nova Hora")
            .build()

        timePicker.addOnPositiveButtonClickListener {
            calendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            calendar.set(Calendar.MINUTE, timePicker.minute)
            
            // Update Firestore
            val newTimestamp = com.google.firebase.Timestamp(calendar.time)
            reuniaoId?.let { id ->
                firestore.collection("reunioes").document(id)
                    .update("dataHora", newTimestamp)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Discipulado remarcado com sucesso!", Toast.LENGTH_SHORT).show()
                        carregarDetalhes()
                    }
            }
        }
        timePicker.show(supportFragmentManager, "TIME_PICKER")
    }

    private fun uploadPhotoFromFile(file: File) {
        val rid = reuniaoId ?: return
        val path = "reunioes/${rid}/foto.jpg"
        val ref = storage.reference.child(path)
        Toast.makeText(this, "Comprimindo e enviando foto...", Toast.LENGTH_SHORT).show()

        // Compression logic
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)
        
        // Target max size 1200px
        var scale = 1
        while (options.outWidth / scale / 2 >= 1200 || options.outHeight / scale / 2 >= 1200) {
            scale *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = scale
        }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return
        
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
        val data = baos.toByteArray()

        ref.putBytes(data)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    val updates = mapOf(
                        "fotoUrl" to uri.toString(),
                        "status" to "Realizada"
                    )
                    firestore.collection("reunioes").document(rid)
                        .update(updates)
                        .addOnSuccessListener {
                            carregarDetalhes()
                        }
                    
                    Glide.with(this@DetalhesReuniaoActivity)
                        .load(uri.toString())
                        .into(imagePreview)
                        
                    Toast.makeText(this, "Foto salva com sucesso!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("UploadPhoto", "Erro ao enviar", e)
                Toast.makeText(this, "Erro ao enviar foto: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun toggleAudio() {
        val url = currentReuniao?.audioUrl ?: return

        if (isPlaying) {
            stopAudio()
        } else {
            playAudio(url)
        }
    }

    private fun playAudio(url: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = android.media.MediaPlayer().apply {
                setDataSource(url)
                prepareAsync()
                setOnPreparedListener {
                    start()
                    this@DetalhesReuniaoActivity.isPlaying = true
                    btnPlayAudio.text = "Parar Áudio"
                    btnPlayAudio.setIconResource(R.drawable.ic_stop)
                }
                setOnCompletionListener {
                    stopAudio()
                }
                setOnErrorListener { _, _, _ ->
                    Toast.makeText(this@DetalhesReuniaoActivity, "Erro ao reproduzir áudio", Toast.LENGTH_SHORT).show()
                    stopAudio()
                    true
                }
            }
            btnPlayAudio.text = "Carregando..."
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error", e)
            Toast.makeText(this, "Erro ao carregar áudio", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopAudio() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        this@DetalhesReuniaoActivity.isPlaying = false
        btnPlayAudio.text = "Ouvir Áudio"
        btnPlayAudio.setIconResource(R.drawable.ic_play)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
