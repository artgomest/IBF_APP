package com.ibf.app.ui.agenda

import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ibf.app.R
import com.ibf.app.data.models.Reuniao
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.TimeZone
import java.util.Locale

class AgendarReuniaoActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var autoCompleteMembro: AutoCompleteTextView
    private lateinit var inputData: TextInputEditText
    private lateinit var inputHora: TextInputEditText
    private lateinit var inputLocal: TextInputEditText
    private lateinit var inputObservacoes: TextInputEditText
    private lateinit var btnSalvar: MaterialButton
    private lateinit var btnWhatsapp: MaterialButton
    private lateinit var btnAdicionarAgenda: MaterialButton
    private lateinit var containerSucesso: LinearLayout

    private val calendar = Calendar.getInstance()
    private val membrosMap = mutableMapOf<String, String>() // Nome -> UID
    private val membrosEmailMap = mutableMapOf<String, String>() // Nome -> Email

    private var redeSelecionada: String? = null
    private var membroPreSelecionadoNome: String? = null
    private var membroPreSelecionadoId: String? = null
    private var membroPreSelecionadoEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agendar_reuniao)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        
        redeSelecionada = intent.getStringExtra("REDE_SELECIONADA")
        membroPreSelecionadoNome = intent.getStringExtra("MEMBRO_PRE_SELECIONADO_NOME")
        membroPreSelecionadoId = intent.getStringExtra("MEMBRO_PRE_SELECIONADO_ID")
        membroPreSelecionadoEmail = intent.getStringExtra("MEMBRO_PRE_SELECIONADO_EMAIL")

        setupToolbar()
        initViews()
        carregarMembros()

        btnSalvar.setOnClickListener { salvarReuniao() }
        btnWhatsapp.setOnClickListener { abrirWhatsapp() }
        btnAdicionarAgenda.setOnClickListener { abrirGoogleAgenda() }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun initViews() {
        autoCompleteMembro = findViewById(R.id.autoCompleteMembro)
        inputData = findViewById(R.id.input_data)
        inputHora = findViewById(R.id.input_hora)
        inputLocal = findViewById(R.id.input_local)
        inputObservacoes = findViewById(R.id.input_observacoes)
        btnSalvar = findViewById(R.id.btn_salvar_reuniao)
        btnWhatsapp = findViewById(R.id.btn_convidar_whatsapp)
        btnAdicionarAgenda = findViewById(R.id.btn_adicionar_agenda)
        containerSucesso = findViewById(R.id.container_sucesso)

        inputData.setOnClickListener { showDatePicker() }
        inputHora.setOnClickListener { showTimePicker() }

        // Inicializar com data/hora atual
        updateDisplayDateTime()
    }

    private fun carregarMembros() {
        val uid = auth.currentUser?.uid ?: return
        
        val query = if (redeSelecionada != null) {
            firestore.collection("membros")
                .whereGreaterThan(com.google.firebase.firestore.FieldPath.of("funcoes", redeSelecionada!!), "")
        } else {
            firestore.collection("membros")
        }

        query.get().addOnSuccessListener { documents ->
            val nomes = mutableListOf<String>()
            membrosMap.clear()
            membrosEmailMap.clear()

            for (doc in documents) {
                 val nome = doc.getString("nome") ?: "Sem Nome"
                 val id = doc.id
                 val email = doc.getString("email") ?: ""

                 if (id != uid) {
                     nomes.add(nome)
                     membrosMap[nome] = id
                     membrosEmailMap[nome] = email
                 }
            }

            membroPreSelecionadoNome?.let { nome ->
                membroPreSelecionadoId?.let { id ->
                    if (!membrosMap.containsKey(nome)) {
                        nomes.add(nome)
                        membrosMap[nome] = id
                    }
                    membrosEmailMap[nome] = membroPreSelecionadoEmail ?: membrosEmailMap[nome] ?: ""
                    autoCompleteMembro.setText(nome, false)
                    autoCompleteMembro.isEnabled = false
                }
            }

            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, nomes)
            autoCompleteMembro.setAdapter(adapter)
        }.addOnFailureListener {
            Log.e("Agendar", "Erro ao carregar membros", it)
        }
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Selecione a Data")
            .setSelection(calendar.timeInMillis)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            utcCalendar.timeInMillis = selection
            
            // Usar set com 3 argumentos para ser atômico e evitar bugs de virada de mês
            calendar.set(
                utcCalendar.get(Calendar.YEAR),
                utcCalendar.get(Calendar.MONTH),
                utcCalendar.get(Calendar.DAY_OF_MONTH)
            )
            
            updateDisplayDateTime()
        }
        datePicker.show(supportFragmentManager, "DATE_PICKER")
    }

    private fun showTimePicker() {
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(calendar.get(Calendar.HOUR_OF_DAY))
            .setMinute(calendar.get(Calendar.MINUTE))
            .setTitleText("Selecione a Hora")
            .build()

        timePicker.addOnPositiveButtonClickListener {
            calendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            calendar.set(Calendar.MINUTE, timePicker.minute)
            updateDisplayDateTime()
        }
        timePicker.show(supportFragmentManager, "TIME_PICKER")
    }

    private fun updateDisplayDateTime() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        val timeFormat = SimpleDateFormat("HH:mm", Locale("pt", "BR"))
        
        inputData.setText(dateFormat.format(calendar.time))
        inputHora.setText(timeFormat.format(calendar.time))
    }

    private fun salvarReuniao() {
        val nomeMembro = autoCompleteMembro.text.toString()
        val membroUid = membrosMap[nomeMembro]

        if (membroUid == null) {
            Toast.makeText(this, "Selecione um membro válido", Toast.LENGTH_SHORT).show()
            return
        }

        val local = inputLocal.text.toString()
        if (local.isEmpty()) {
            Toast.makeText(this, "Informe o local do encontro", Toast.LENGTH_SHORT).show()
            return
        }

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
        btnSalvar.text = "Processando..."

        firestore.collection("reunioes")
            .add(reuniao)
            .addOnSuccessListener {
                Toast.makeText(this, "Agenda confirmada!", Toast.LENGTH_SHORT).show()
                btnSalvar.visibility = View.GONE
                containerSucesso.visibility = View.VISIBLE
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao agendar: ${it.message}", Toast.LENGTH_SHORT).show()
                btnSalvar.isEnabled = true
                btnSalvar.text = "Confirmar Agendamento"
            }
    }

    private fun abrirWhatsapp() {
        val nomeMembro = autoCompleteMembro.text.toString()
        val format = SimpleDateFormat("dd/MM 'às' HH:mm", Locale("pt", "BR"))
        val dataStr = format.format(calendar.time)
        val local = inputLocal.text.toString()
        val message = "Olá $nomeMembro, marquei nosso discipulado para $dataStr no local: $local. Confirma?"
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        startActivity(Intent.createChooser(intent, "Compartilhar convite..."))
    }

    private fun abrirGoogleAgenda() {
        val nomeMembro = autoCompleteMembro.text.toString()
        val local = inputLocal.text.toString()
        val obs = inputObservacoes.text.toString()
        val emailMembro = membrosEmailMap[nomeMembro] ?: ""

        val startMs = calendar.timeInMillis
        val endMs = startMs + 60 * 60 * 1000L // 1 hora padrão

        try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, "Discipulado com $nomeMembro")
                putExtra(CalendarContract.Events.EVENT_LOCATION, local)
                putExtra(CalendarContract.Events.DESCRIPTION, if (obs.isNotEmpty()) obs else "Discipulado agendado pelo IBF App")
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMs)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMs)
                if (emailMembro.isNotEmpty()) {
                    putExtra(Intent.EXTRA_EMAIL, emailMembro)
                }
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Nenhum app de calendário disponível.", Toast.LENGTH_SHORT).show()
        }
    }
}
