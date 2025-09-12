package com.ibf.app.ui.usuarios

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.ibf.app.R
import java.util.Calendar

class FichaMembroActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private var membroId: String? = null

    // Declaração dos componentes da UI
    private lateinit var textPageTitle: TextView
    private lateinit var textNomeMembro: TextInputEditText
    private lateinit var textDataNascimento: TextInputEditText
    private lateinit var textCpf: TextInputEditText
    private lateinit var spinnerEstadoCivil: Spinner
    private lateinit var layoutNomeConjuge: TextInputLayout
    private lateinit var textNomeConjuge: TextInputEditText
    private lateinit var layoutNumeroFilhos: TextInputLayout
    private lateinit var textNumeroFilhos: TextInputEditText
    private lateinit var buttonSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ficha_membro)

        firestore = FirebaseFirestore.getInstance()
        membroId = intent.getStringExtra("MEMBRO_ID")

        // Inicializa os componentes da UI
        textPageTitle = findViewById(R.id.text_page_title)
        textNomeMembro = findViewById(R.id.text_nome_membro)
        textDataNascimento = findViewById(R.id.text_data_nascimento)
        textCpf = findViewById(R.id.text_cpf)
        spinnerEstadoCivil = findViewById(R.id.spinner_estado_civil)
        layoutNomeConjuge = findViewById(R.id.layout_nome_conjuge)
        textNomeConjuge = findViewById(R.id.text_nome_conjuge)
        layoutNumeroFilhos = findViewById(R.id.layout_numero_filhos)
        textNumeroFilhos = findViewById(R.id.text_numero_filhos)
        buttonSave = findViewById(R.id.button_save)

        if (membroId == null) {
            Toast.makeText(this, "Erro: ID do membro não fornecido.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        configurarSpinner()
        configurarDatePicker()
        configurarBotoes()
        carregarDadosDoMembro()
    }

    private fun configurarDatePicker() {
        textDataNascimento.isFocusable = false
        textDataNascimento.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = "${selectedDay.toString().padStart(2, '0')}/${(selectedMonth + 1).toString().padStart(2, '0')}/$selectedYear"
                textDataNascimento.setText(selectedDate)
            }, year, month, day)
            datePickerDialog.show()
        }
    }

    private fun configurarSpinner() {
        val estadosCivis = arrayOf("Solteiro(a)", "Casado(a)", "Divorciado(a)", "Viúvo(a)")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, estadosCivis)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerEstadoCivil.adapter = adapter

        spinnerEstadoCivil.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val isCasado = estadosCivis[position] == "Casado(a)"
                layoutNomeConjuge.isVisible = isCasado
                layoutNumeroFilhos.isVisible = isCasado
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                layoutNomeConjuge.isVisible = false
                layoutNumeroFilhos.isVisible = false
            }
        }
    }

    private fun configurarBotoes() {
        findViewById<ImageView>(R.id.button_back).setOnClickListener { finish() }

        findViewById<FloatingActionButton>(R.id.fab_agendar_discipulado).setOnClickListener {
            Toast.makeText(this, "Agendamento de discipulado em breve!", Toast.LENGTH_SHORT).show()
        }

        buttonSave.setOnClickListener {
            salvarDadosDoMembro()
        }
    }

    private fun carregarDadosDoMembro() {
        membroId?.let { id ->
            firestore.collection("usuarios").document(id).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val nome = document.getString("nome") ?: "Nome não encontrado"
                        val dataNascimento = document.getString("dataNascimento") ?: ""
                        val cpf = document.getString("cpf") ?: ""
                        val estadoCivil = document.getString("estadoCivil") ?: "Solteiro(a)"
                        val nomeConjuge = document.getString("nomeConjuge") ?: ""
                        val numeroFilhos = document.getLong("numeroFilhos")?.toString() ?: ""


                        // Preenche os campos da UI com os dados do Firestore
                        textPageTitle.text = nome
                        textNomeMembro.setText(nome)
                        textDataNascimento.setText(dataNascimento)
                        textCpf.setText(cpf)

                        (spinnerEstadoCivil.adapter as? ArrayAdapter<String>)?.let {
                            val position = it.getPosition(estadoCivil)
                            if (position >= 0) {
                                spinnerEstadoCivil.setSelection(position)
                            }
                        }

                        textNomeConjuge.setText(nomeConjuge)
                        textNumeroFilhos.setText(numeroFilhos)

                    } else {
                        Toast.makeText(this, "Membro não encontrado.", Toast.LENGTH_LONG).show()
                        Log.w("FichaMembroActivity", "Nenhum documento encontrado com o ID: $id")
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao carregar dados do membro.", Toast.LENGTH_SHORT).show()
                    Log.e("FichaMembroActivity", "Erro ao buscar documento: ${e.message}", e)
                }
        }
    }

    private fun salvarDadosDoMembro() {
        membroId?.let { id ->
            val dadosMembro = mapOf(
                "nome" to textNomeMembro.text.toString(),
                "dataNascimento" to textDataNascimento.text.toString(),
                "cpf" to textCpf.text.toString(),
                "estadoCivil" to spinnerEstadoCivil.selectedItem.toString(),
                "nomeConjuge" to if (layoutNomeConjuge.isVisible) textNomeConjuge.text.toString() else null,
                "numeroFilhos" to if (layoutNumeroFilhos.isVisible) textNumeroFilhos.text.toString().toIntOrNull() else null
            ).filterValues { it != null }

            firestore.collection("usuarios").document(id).update(dadosMembro)
                .addOnSuccessListener {
                    Toast.makeText(this, "Dados do membro atualizados com sucesso!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao atualizar dados.", Toast.LENGTH_SHORT).show()
                    Log.e("FichaMembroActivity", "Erro ao atualizar documento: ${e.message}", e)
                }
        }
    }
}
