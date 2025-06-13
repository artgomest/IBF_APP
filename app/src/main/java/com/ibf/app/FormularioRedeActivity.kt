package com.ibf.app

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FormularioRedeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var formData: TextView
    private lateinit var buttonEnviar: Button
    private lateinit var formRede: EditText
    private lateinit var formNome: EditText

    private var relatorioId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_formulario_rede)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        formNome = findViewById(R.id.formNome)
        formRede = findViewById(R.id.formRede)
        formData = findViewById(R.id.formData)
        buttonEnviar = findViewById(R.id.buttonEnviar)

        // Pega as informações que vieram da tela anterior
        val redePreenchida = intent.getStringExtra("REDE_SELECIONADA")
        val dataPendente = intent.getStringExtra("DATA_PENDENTE")
        relatorioId = intent.getStringExtra("RELATORIO_ID")

        if (relatorioId != null) {
            // MODO EDIÇÃO: Carrega um relatório existente para editar
            buttonEnviar.text = getString(R.string.form_button_atualizar)
            carregarDadosDoRelatorio(relatorioId!!)
        } else {
            // MODO CRIAÇÃO: Cria um relatório novo ou preenche um pendente
            buttonEnviar.text = getString(R.string.form_button_enviar)
            carregarDadosDoUsuario()

            // Verifica se é um relatório PENDENTE ou totalmente NOVO
            if (dataPendente != null) {
                // Se for PENDENTE, preenche e bloqueia AMBOS os campos
                formRede.setText(redePreenchida)
                formData.text = dataPendente

                formRede.isEnabled = false
                formData.isEnabled = false
            } else {
                // Se for NOVO, preenche com os dados padrão e mantém editável
                formRede.setText(redePreenchida)
                formRede.isEnabled = true // ALTERADO: Permite edição

                val calendario = Calendar.getInstance()
                val formato = "dd/MM/yyyy"
                val sdf = SimpleDateFormat(formato, Locale.getDefault())
                formData.text = sdf.format(calendario.time)
                formData.isEnabled = true // Permite edição
            }
        }

        // Configura o seletor de data. Ele só funcionará se o campo formData estiver habilitado.
        configurarSeletorDeData()

        buttonEnviar.setOnClickListener {
            enviarRelatorio()
        }
    }

    private fun carregarDadosDoUsuario() {
        val user = auth.currentUser
        if (user != null) {
            firestore.collection("usuarios").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        formNome.setText(document.getString("nome"))
                    }
                }
        }
    }

    private fun carregarDadosDoRelatorio(id: String) {
        firestore.collection("relatorios").document(id).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    formNome.setText(document.getString("autorNome"))
                    formRede.setText(document.getString("idRede"))
                    formRede.isEnabled = false // Também bloqueia a rede na edição

                    formData.text = document.getString("dataReuniao")
                    findViewById<EditText>(R.id.formDescricao).setText(document.getString("descricao"))
                    findViewById<EditText>(R.id.formPessoas).setText(document.getLong("totalPessoas")?.toString() ?: "")
                    findViewById<EditText>(R.id.formVisitantes).setText(document.getLong("totalVisitantes")?.toString() ?: "")
                    findViewById<EditText>(R.id.formOferta).setText(document.getDouble("valorOferta")?.toString() ?: "")
                    findViewById<EditText>(R.id.formComentarios).setText(document.getString("comentarios"))
                }
            }
    }

    private fun enviarRelatorio() {
        val user = auth.currentUser ?: return

        // Validação básica para garantir que campos essenciais não estão vazios
        if (formData.text.isEmpty() || formRede.text.isEmpty()) {
            Toast.makeText(this, "Data e Rede são campos obrigatórios.", Toast.LENGTH_SHORT).show()
            return
        }

        val relatorioMap = hashMapOf<String, Any?>(
            "autorUid" to user.uid,
            "autorNome" to formNome.text.toString(),
            "idRede" to formRede.text.toString(),
            "dataReuniao" to formData.text.toString(),
            "descricao" to findViewById<EditText>(R.id.formDescricao).text.toString(),
            "totalPessoas" to findViewById<EditText>(R.id.formPessoas).text.toString().toIntOrNull(),
            "totalVisitantes" to findViewById<EditText>(R.id.formVisitantes).text.toString().toIntOrNull(),
            "valorOferta" to findViewById<EditText>(R.id.formOferta).text.toString().toDoubleOrNull(),
            "comentarios" to findViewById<EditText>(R.id.formComentarios).text.toString(),
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        if (relatorioId != null) {
            firestore.collection("relatorios").document(relatorioId!!)
                .set(relatorioMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Relatório atualizado com sucesso!", Toast.LENGTH_LONG).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao atualizar relatório: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            firestore.collection("relatorios")
                .add(relatorioMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Relatório enviado com sucesso!", Toast.LENGTH_LONG).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao enviar relatório: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun configurarSeletorDeData() {
        val calendario = Calendar.getInstance()
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendario.set(Calendar.YEAR, year)
            calendario.set(Calendar.MONTH, month)
            calendario.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            val formato = "dd/MM/yyyy"
            val sdf = SimpleDateFormat(formato, Locale.getDefault())
            formData.text = sdf.format(calendario.time)
        }

        formData.setOnClickListener {
            DatePickerDialog(this, dateSetListener, calendario.get(Calendar.YEAR), calendario.get(Calendar.MONTH), calendario.get(Calendar.DAY_OF_MONTH)).show()
        }
    }
}