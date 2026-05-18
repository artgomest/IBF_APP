package com.ibf.app.ui.configuracoes

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.ibf.app.R

class EditarRedeActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private var redeSelecionada: String? = null
    private var documentoIdDaRede: String? = null

    private lateinit var editTextNomeRede: TextInputEditText
    private lateinit var editTextHorario: TextInputEditText
    private lateinit var spinnerDiaSemana: Spinner
    private lateinit var buttonSalvar: Button

    // Flag para evitar loop no TextWatcher de formatação de horário
    private var isFormattingHorario = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_rede)

        firestore = FirebaseFirestore.getInstance()
        redeSelecionada = intent.getStringExtra("REDE_SELECIONADA")

        if (redeSelecionada == null) {
            Toast.makeText(this, "Erro: Rede não especificada.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        editTextNomeRede = findViewById(R.id.edit_text_nome_rede)
        editTextHorario = findViewById(R.id.edit_text_horario_reuniao)
        spinnerDiaSemana = findViewById(R.id.spinner_dia_semana)
        buttonSalvar = findViewById(R.id.button_salvar_rede)
        findViewById<ImageView>(R.id.button_back).setOnClickListener { finish() }

        configurarSpinner()
        configurarHorarioAutoFormat()
        carregarDadosDaRede()

        buttonSalvar.setOnClickListener {
            salvarAlteracoes()
        }
    }

    private fun configurarSpinner() {
        val dias = listOf("Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, dias)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDiaSemana.adapter = adapter
    }

    /**
     * Formata automaticamente o campo de horário para HH:MM enquanto o usuário digita.
     */
    private fun configurarHorarioAutoFormat() {
        editTextHorario.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormattingHorario || s == null) return
                isFormattingHorario = true

                val digits = s.toString().filter { it.isDigit() }.take(4)
                val formatted = when {
                    digits.length >= 3 -> "${digits.substring(0, 2)}:${digits.substring(2)}"
                    else -> digits
                }

                editTextHorario.setText(formatted)
                editTextHorario.setSelection(formatted.length)
                isFormattingHorario = false
            }
        })
    }

    private fun carregarDadosDaRede() {
        firestore.collection("redes")
            .whereEqualTo("nome", redeSelecionada)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.first()
                    documentoIdDaRede = document.id

                    val nome = document.getString("nome")
                    val diaDaSemana = document.getLong("diaDaSemana")?.toInt()
                    val horario = document.getString("horarioReuniao")

                    editTextNomeRede.setText(nome)

                    if (diaDaSemana != null && diaDaSemana > 0 && diaDaSemana <= 7) {
                        spinnerDiaSemana.setSelection(diaDaSemana - 1)
                    }

                    if (!horario.isNullOrEmpty()) {
                        editTextHorario.setText(horario)
                    }
                } else {
                    Toast.makeText(this, getString(R.string.falha_carregar_dados_rede), Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.falha_carregar_dados_rede), Toast.LENGTH_SHORT).show()
            }
    }

    private fun salvarAlteracoes() {
        if (documentoIdDaRede == null) {
            Toast.makeText(this, "Erro: Não foi possível identificar a rede para salvar.", Toast.LENGTH_SHORT).show()
            return
        }

        val novoNome = editTextNomeRede.text.toString().trim()
        val novoDiaDaSemana = spinnerDiaSemana.selectedItemPosition + 1
        val novoHorario = editTextHorario.text.toString().trim()

        if (novoNome.isEmpty()) {
            Toast.makeText(this, "O nome da rede não pode ficar vazio.", Toast.LENGTH_SHORT).show()
            return
        }

        // Valida formato HH:MM
        if (novoHorario.isNotEmpty() && !novoHorario.matches(Regex("^([01]\\d|2[0-3]):[0-5]\\d$"))) {
            Toast.makeText(this, "Horário inválido. Use o formato HH:MM (ex: 19:30).", Toast.LENGTH_SHORT).show()
            return
        }

        val dadosAtualizados = hashMapOf(
            "nome" to novoNome,
            "diaDaSemana" to novoDiaDaSemana,
            "horarioReuniao" to novoHorario
        )

        firestore.collection("redes").document(documentoIdDaRede!!)
            .update(dadosAtualizados as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(this, getString(R.string.rede_atualizada_sucesso), Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.falha_atualizar_rede), Toast.LENGTH_SHORT).show()
            }
    }
}