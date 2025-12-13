package com.ibf.app.ui.agenda

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ibf.app.R
import com.ibf.app.data.models.Reuniao
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AgendarReuniaoActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var autoCompleteMembro: AutoCompleteTextView
    private lateinit var inputLocal: TextInputEditText
    private lateinit var inputObservacoes: TextInputEditText
    private lateinit var textData: android.widget.TextView
    private lateinit var btnSalvar: MaterialButton
    private lateinit var btnWhatsapp: MaterialButton

    private val calendar = Calendar.getInstance()
    private val membrosMap = mutableMapOf<String, String>() // Nome -> UID
    private val membrosCelularMap = mutableMapOf<String, String>() // Nome -> Celular

    private var redeSelecionada: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agendar_reuniao)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        redeSelecionada = intent.getStringExtra("REDE_SELECIONADA")

        initViews()
        carregarMembros()
        setupPickers()

        btnSalvar.setOnClickListener { salvarReuniao() }
        btnWhatsapp.setOnClickListener { abrirWhatsapp() }
    }

    private fun initViews() {
        autoCompleteMembro = findViewById(R.id.autoCompleteMembro)
        inputLocal = findViewById(R.id.input_local)
        inputObservacoes = findViewById(R.id.input_observacoes)
        textData = findViewById(R.id.text_data_selecionada)
        btnSalvar = findViewById(R.id.btn_salvar_reuniao)
        btnWhatsapp = findViewById(R.id.btn_convidar_whatsapp)

        findViewById<MaterialButton>(R.id.btn_data).setOnClickListener { pickDate() }
        findViewById<MaterialButton>(R.id.btn_hora).setOnClickListener { pickTime() }
        
        updateDateText()
    }

    private fun carregarMembros() {
        // Load members of the leader's network to populate the dropdown
        val uid = auth.currentUser?.uid ?: return
        
        // Assuming current user is leader and we list members where 'liderId' == uid
        // Or using the 'rede' logic if that's how it's structured. 
        // For MVP, checking members in the same 'rede' or assigned to this leader.
        // Let's reuse logic similar to MembrosRedeActivity but keeping it simple: fetch all users where map 'lider' equals current user or check 'rede'.
        // To be safe and broadly compatible, let's query users in the same network context.
        
        val query = firestore.collection("usuarios")
        // If we strictly follow hierarchy, we filter by liderUid. If flat network, by rede.
        // Using 'rede' if available, otherwise fallback.
        if (redeSelecionada != null) {
            query.whereEqualTo("rede", redeSelecionada)
        } else {
            // Fallback: This might be too broad if not careful, but for now filtering locally or by a known field is safest.
            // Let's assume we want all users for now and filter in memory if needed, or better, query by liderUid if that field exists on members.
            // Based on previous files, 'rede' seems the key.
             query.whereEqualTo("rede", redeSelecionada) // This might be null, handling below.
        }

        query.get().addOnSuccessListener { documents ->
            val nomes = mutableListOf<String>()
            membrosMap.clear()
            membrosCelularMap.clear()

            for (doc in documents) {
                 val nome = doc.getString("nome") ?: "Sem Nome"
                 val id = doc.id
                 val celular = doc.getString("celular") ?: ""
                 
                 // Don't list yourself
                 if (id != uid) {
                     nomes.add(nome)
                     membrosMap[nome] = id
                     membrosCelularMap[nome] = celular
                 }
            }

            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, nomes)
            autoCompleteMembro.setAdapter(adapter)
        }.addOnFailureListener {
            Log.e("Agendar", "Erro ao carregar membros", it)
        }
    }

    private fun pickDate() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                updateDateText()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun pickTime() {
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                updateDateText()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.show()
    }

    private fun updateDateText() {
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
        textData.text = format.format(calendar.time)
    }

    private fun salvarReuniao() {
        val nomeMembro = autoCompleteMembro.text.toString()
        val membroUid = membrosMap[nomeMembro]

        if (membroUid == null) {
            Toast.makeText(this, "Selecione um membro válido", Toast.LENGTH_SHORT).show()
            return
        }

        val local = inputLocal.text.toString()
        val obs = inputObservacoes.text.toString()
        val dataHora = Timestamp(calendar.time)

        val reuniao = Reuniao(
            liderUid = auth.currentUser?.uid ?: "",
            membroUid = membroUid,
            membroNome = nomeMembro,
            dataHora = dataHora,
            local = local,
            observacoes = obs,
            status = "Agendada",
            rede = redeSelecionada ?: ""
        )

        btnSalvar.isEnabled = false
        btnSalvar.text = "Agendando..."

        firestore.collection("reunioes")
            .add(reuniao)
            .addOnSuccessListener {
                Toast.makeText(this, "Agenda criada com sucesso!", Toast.LENGTH_SHORT).show()
                btnSalvar.visibility = View.GONE
                btnWhatsapp.visibility = View.VISIBLE
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao agendar: ${it.message}", Toast.LENGTH_SHORT).show()
                btnSalvar.isEnabled = true
                btnSalvar.text = "Agendar Discipulado"
            }
    }

    private fun abrirWhatsapp() {
        val nomeMembro = autoCompleteMembro.text.toString()
        val celular = membrosCelularMap[nomeMembro] ?: ""
        
        if (celular.isEmpty()) {
            Toast.makeText(this, "Membro sem celular cadastrado.", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Format number (simple check, assuming BR +55 or need to add)
        // Usually stored as pure digits. WhatsApp API needs country code.
        // Assuming localized usage, we might prepend 55 if length is 10 or 11.
        
        var phone = celular.replace(Regex("[^0-9]"), "")
        if (phone.length <= 11 && !phone.startsWith("55")) {
             phone = "55$phone"
        }

        val format = SimpleDateFormat("dd/MM 'às' HH:mm", Locale("pt", "BR"))
        val dataStr = format.format(calendar.time)
        val local = inputLocal.text.toString()
        
        val message = "Olá $nomeMembro, marquei nosso discipulado para $dataStr no local: $local. Confirma?"
        
        try {
            val url = "https://api.whatsapp.com/send?phone=$phone&text=${URLEncoder.encode(message, "UTF-8")}"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao abrir WhatsApp.", Toast.LENGTH_SHORT).show()
        }
    }
}
