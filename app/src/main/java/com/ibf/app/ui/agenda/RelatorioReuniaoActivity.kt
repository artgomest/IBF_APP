package com.ibf.app.ui.agenda

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaRecorder
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.ibf.app.R
import com.ibf.app.data.models.Reuniao
import java.io.File
import java.io.IOException
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RelatorioReuniaoActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var reuniaoId: String? = null
    private var reuniao: com.ibf.app.data.models.Reuniao? = null

    // UI
    private lateinit var textInfoMembro: TextView
    private lateinit var textInfoData: TextView
    private lateinit var tabTexto: MaterialButton
    private lateinit var tabAudio: MaterialButton
    private lateinit var sectionTexto: View
    private lateinit var sectionAudio: View
    private lateinit var inputResumo: TextInputEditText
    private lateinit var textTimer: TextView
    private lateinit var progressAudio: ProgressBar
    private lateinit var btnGravarAudio: MaterialButton
    private lateinit var btnPararAudio: MaterialButton
    private lateinit var textAudioStatus: TextView
    private lateinit var btnSalvar: MaterialButton

    // Audio recording state
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false
    private var countDownTimer: CountDownTimer? = null
    private val maxAudioSeconds = 120L
    private var isAudioMode = false

    private val AUDIO_PERMISSION_REQUEST = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_relatorio_reuniao)

        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        reuniaoId = intent.getStringExtra("REUNIAO_ID")

        if (reuniaoId == null) { finish(); return }

        initViews()
        carregarInfoReuniao()
    }

    private fun initViews() {
        textInfoMembro = findViewById(R.id.text_info_membro)
        textInfoData = findViewById(R.id.text_info_data)
        tabTexto = findViewById(R.id.tab_texto)
        tabAudio = findViewById(R.id.tab_audio)
        sectionTexto = findViewById(R.id.section_texto)
        sectionAudio = findViewById(R.id.section_audio)
        inputResumo = findViewById(R.id.input_resumo)
        textTimer = findViewById(R.id.text_timer)
        progressAudio = findViewById(R.id.progress_audio)
        btnGravarAudio = findViewById(R.id.btn_gravar_audio)
        btnPararAudio = findViewById(R.id.btn_parar_audio)
        textAudioStatus = findViewById(R.id.text_audio_status)
        btnSalvar = findViewById(R.id.btn_salvar_relatorio)

        findViewById<ImageView>(R.id.button_back).setOnClickListener { finish() }

        tabTexto.setOnClickListener { setTab(audioMode = false) }
        tabAudio.setOnClickListener { setTab(audioMode = true) }

        btnGravarAudio.setOnClickListener { startAudioRecording() }
        btnPararAudio.setOnClickListener { stopAudioRecording() }
        btnSalvar.setOnClickListener { salvarRelatorio() }

        setTab(audioMode = false)
    }

    private fun setTab(audioMode: Boolean) {
        isAudioMode = audioMode
        val accentColor = ContextCompat.getColor(this, R.color.dark_accent)
        val secondaryColor = ContextCompat.getColor(this, R.color.dark_text_secondary)

        if (!audioMode) {
            // Text tab selected
            sectionTexto.visibility = View.VISIBLE
            sectionAudio.visibility = View.GONE
            tabTexto.backgroundTintList = android.content.res.ColorStateList.valueOf(accentColor)
            tabTexto.setTextColor(Color.WHITE)
            tabAudio.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
            tabAudio.setTextColor(secondaryColor)
        } else {
            // Audio tab selected
            sectionTexto.visibility = View.GONE
            sectionAudio.visibility = View.VISIBLE
            tabAudio.backgroundTintList = android.content.res.ColorStateList.valueOf(accentColor)
            tabAudio.setTextColor(Color.WHITE)
            tabTexto.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
            tabTexto.setTextColor(secondaryColor)
        }
    }

    private fun carregarInfoReuniao() {
        reuniaoId?.let { id ->
            firestore.collection("reunioes").document(id).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val r = doc.toObject(Reuniao::class.java)
                        if (r != null) {
                            reuniao = r
                            textInfoMembro.text = r.membroNome
                            val fmt = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))
                            textInfoData.text = r.dataHora?.toDate()?.let { fmt.format(it) } ?: "—"
                        }
                    }
                }
        }
    }

    private fun startAudioRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO), AUDIO_PERMISSION_REQUEST
            )
            return
        }

        audioFile = File(externalCacheDir, "relatorio_${System.currentTimeMillis()}.mp4")
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFile!!.absolutePath)
            try {
                prepare()
                start()
                isRecording = true
                btnGravarAudio.isEnabled = false
                btnPararAudio.isEnabled = true
                textAudioStatus.text = "Gravando..."
                startCountdown()
            } catch (e: IOException) {
                Log.e("RelatorioAudio", "prepare() failed", e)
                Toast.makeText(this@RelatorioReuniaoActivity, "Erro ao iniciar gravação.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCountdown() {
        progressAudio.max = maxAudioSeconds.toInt()
        progressAudio.progress = maxAudioSeconds.toInt()
        countDownTimer = object : CountDownTimer(maxAudioSeconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secsLeft = millisUntilFinished / 1000L
                textTimer.text = String.format("%02d:%02d", secsLeft / 60, secsLeft % 60)
                progressAudio.progress = secsLeft.toInt()
            }
            override fun onFinish() {
                textTimer.text = "00:00"
                progressAudio.progress = 0
                if (isRecording) {
                    stopAudioRecording()
                    Toast.makeText(
                        this@RelatorioReuniaoActivity,
                        "Limite de 2 minutos atingido.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }

    private fun stopAudioRecording() {
        countDownTimer?.cancel()
        countDownTimer = null
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e("RelatorioAudio", "stop() error", e)
        }
        mediaRecorder?.release()
        mediaRecorder = null
        isRecording = false
        btnGravarAudio.isEnabled = true
        btnPararAudio.isEnabled = false
        textAudioStatus.text = "Gravação pronta. Clique em Salvar."
    }

    private fun salvarRelatorio() {
        if (isAudioMode) {
            val file = audioFile
            if (file == null || !file.exists()) {
                Toast.makeText(this, "Grave um áudio primeiro.", Toast.LENGTH_SHORT).show()
                return
            }
            if (isRecording) stopAudioRecording()
            uploadAudioAndSave(file)
        } else {
            val resumo = inputResumo.text?.toString()?.trim() ?: ""
            if (resumo.isEmpty()) {
                inputResumo.error = "Preencha o resumo do discipulado."
                return
            }
            salvarTextoFirestore(resumo)
        }
    }

    private fun uploadAudioAndSave(file: File) {
        btnSalvar.isEnabled = false
        btnSalvar.text = "Enviando áudio..."
        textAudioStatus.text = "Fazendo upload..."

        val expiresAt = Calendar.getInstance().apply { add(Calendar.MONTH, 2) }.timeInMillis
        val metadata = StorageMetadata.Builder()
            .setContentType("audio/mp4")
            .setCustomMetadata("expiresAt", expiresAt.toString())
            .build()

        // Build path: Discipulados/{rede}/{membroNome}/{dataHora}.3gp
        fun String.sanitize() = this.replace("/", "-").replace(" ", "_").trim()
        val rede = (reuniao?.rede?.sanitize()?.ifEmpty { "sem_rede" }) ?: "sem_rede"
        val membro = (reuniao?.membroNome?.sanitize()?.ifEmpty { "membro" }) ?: "membro"
        val dtFmt = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
        val dtStr = reuniao?.dataHora?.toDate()?.let { dtFmt.format(it) } ?: dtFmt.format(Date())
        val ref = storage.reference.child("Discipulados/$rede/$membro/$dtStr.mp4")

        // Use putStream instead of putFile(Uri) to avoid URI permission issues on Android 7+
        val inputStream = try {
            file.inputStream()
        } catch (e: Exception) {
            Log.e("RelatorioAudio", "Cannot open audio file", e)
            Toast.makeText(this, "Erro ao ler arquivo de áudio.", Toast.LENGTH_SHORT).show()
            btnSalvar.isEnabled = true
            btnSalvar.text = "Salvar Relatório"
            return
        }

        ref.putStream(inputStream, metadata)
            .addOnSuccessListener {
                inputStream.close()
                ref.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        salvarComAudioFirestore(downloadUri.toString())
                    }
                    .addOnFailureListener { e ->
                        Log.e("RelatorioAudio", "downloadUrl failed", e)
                        // Audio uploaded but URL retrieval failed — save without URL
                        Toast.makeText(this, "Áudio salvo. URL não disponível.", Toast.LENGTH_SHORT).show()
                        salvarComAudioFirestore("")
                    }
            }
            .addOnFailureListener { e ->
                inputStream.close()
                Log.e("RelatorioAudio", "Upload failed", e)
                Toast.makeText(this, "Erro no upload: ${e.message}", Toast.LENGTH_LONG).show()
                textAudioStatus.text = "Falha no envio. Verifique as regras do Firebase Storage."
                btnSalvar.isEnabled = true
                btnSalvar.text = "Salvar Relatório"
            }
    }

    private fun salvarTextoFirestore(resumo: String) {
        btnSalvar.isEnabled = false
        btnSalvar.text = "Salvando..."
        val updates = mapOf(
            "resumo" to resumo,
            "status" to "Concluída",
            "relatorioPreenchido" to true
        )
        firestore.collection("reunioes").document(reuniaoId!!)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Relatório salvo!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao salvar: ${it.message}", Toast.LENGTH_SHORT).show()
                btnSalvar.isEnabled = true
                btnSalvar.text = "Salvar Relatório"
            }
    }

    private fun salvarComAudioFirestore(audioUrl: String) {
        val updates = mapOf(
            "audioUrl" to audioUrl,
            "status" to "Concluída",
            "relatorioPreenchido" to true
        )
        firestore.collection("reunioes").document(reuniaoId!!)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Relatório com áudio salvo!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao salvar: ${it.message}", Toast.LENGTH_SHORT).show()
                btnSalvar.isEnabled = true
                btnSalvar.text = "Salvar Relatório"
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        try { mediaRecorder?.stop() } catch (_: Exception) {}
        mediaRecorder?.release()
        mediaRecorder = null
    }
}
