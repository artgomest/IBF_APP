package com.ibf.app.ui.agenda

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.MediaStore
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
import java.util.Locale

class DetalhesReuniaoActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var reuniaoId: String? = null

    private lateinit var imagePreview: ImageView
    private lateinit var btnPreencherRelatorio: MaterialButton
    private lateinit var textRelatorioOk: TextView
    private lateinit var textStatusBadge: TextView

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                imagePreview.setImageBitmap(bitmap)
                uploadPhoto(bitmap)
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

        findViewById<ImageView>(R.id.button_back).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btn_photo).setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
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

        // Show "Preencher Relatório" button only after meeting has passed
        val dataReuniao = r.dataHora?.toDate()
        if (dataReuniao != null && dataReuniao.before(Date())) {
            if (r.relatorioPreenchido) {
                btnPreencherRelatorio.visibility = View.GONE
                textRelatorioOk.visibility = View.VISIBLE
            } else {
                btnPreencherRelatorio.visibility = View.VISIBLE
                textRelatorioOk.visibility = View.GONE
            }
        } else {
            btnPreencherRelatorio.visibility = View.GONE
            textRelatorioOk.visibility = View.GONE
        }
    }

    private fun uploadPhoto(bitmap: Bitmap) {
        val ref = storage.reference.child("reunioes/${reuniaoId}/foto.jpg")
        val baos = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        Toast.makeText(this, "Enviando foto...", Toast.LENGTH_SHORT).show()
        ref.putBytes(baos.toByteArray())
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    firestore.collection("reunioes").document(reuniaoId!!)
                        .update("fotoUrl", uri.toString())
                    Toast.makeText(this, "Foto salva!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao enviar foto.", Toast.LENGTH_SHORT).show()
            }
    }
}
