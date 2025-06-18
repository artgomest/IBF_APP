package com.ibf.app.ui.configuracoes

import android.os.Bundle
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
    private lateinit var spinnerDiaSemana: Spinner
    private lateinit var buttonSalvar: Button

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
        spinnerDiaSemana = findViewById(R.id.spinner_dia_semana)
        buttonSalvar = findViewById(R.id.button_salvar_rede)
        findViewById<ImageView>(R.id.button_back).setOnClickListener { finish() }

        configurarSpinner()
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

                    editTextNomeRede.setText(nome)
                    if (diaDaSemana != null && diaDaSemana > 0 && diaDaSemana <= 7) {
                        spinnerDiaSemana.setSelection(diaDaSemana - 1)
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

        if (novoNome.isEmpty()) {
            Toast.makeText(this, "O nome da rede não pode ficar vazio.", Toast.LENGTH_SHORT).show()
            return
        }

        val dadosAtualizados = hashMapOf(
            "nome" to novoNome,
            "diaDaSemana" to novoDiaDaSemana
        )

        // --- CORREÇÃO APLICADA AQUI ---
        // Adicionamos a conversão explícita para o tipo que o Firestore espera.
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