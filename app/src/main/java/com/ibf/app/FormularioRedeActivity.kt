package com.ibf.app

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FormularioRedeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private var relatorioId: String? = null
    private var redeSelecionada: String? = null
    private var dataPendente: String? = null

    private lateinit var editTextNome: EditText
    private lateinit var editTextRede: EditText
    private lateinit var textViewData: TextView
    private lateinit var editTextDescricao: EditText
    private lateinit var editTextTotalPessoas: EditText
    private lateinit var editTextTotalVisitantes: EditText
    private lateinit var editTextValorOferta: EditText
    private lateinit var editTextComentarios: EditText
    private lateinit var buttonEnviar: Button

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR")) as DecimalFormat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_formulario_rede)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        editTextNome = findViewById(R.id.formNome)
        editTextRede = findViewById(R.id.formRede)
        textViewData = findViewById(R.id.text_data_reuniao_exibida)
        editTextDescricao = findViewById(R.id.formDescricao)
        editTextTotalPessoas = findViewById(R.id.formPessoas)
        editTextTotalVisitantes = findViewById(R.id.formVisitantes)
        editTextValorOferta = findViewById(R.id.formOferta)
        editTextComentarios = findViewById(R.id.formComentarios)
        buttonEnviar = findViewById(R.id.buttonEnviar)

        findViewById<TextView>(R.id.text_page_title)?.text = getString(R.string.form_title)
        findViewById<ImageView>(R.id.button_back)?.setOnClickListener { finish() }

        currencyFormatter.applyPattern("R$ #,##0.00")

        relatorioId = intent.getStringExtra("RELATORIO_ID")
        redeSelecionada = intent.getStringExtra("REDE_SELECIONADA")
        dataPendente = intent.getStringExtra("DATA_PENDENTE")

        if (redeSelecionada == null) {
            Toast.makeText(this, "Erro: Rede não especificada para o formulário.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        editTextRede.setText(redeSelecionada)
        editTextRede.isEnabled = false

        val dataReuniaoParaExibir = dataPendente ?: SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        textViewData.text = dataReuniaoParaExibir

        textViewData.setOnClickListener {
            Toast.makeText(this, "Data da reunião: $dataReuniaoParaExibir", Toast.LENGTH_SHORT).show()
        }

        editTextValorOferta.addTextChangedListener(MoneyTextWatcher(editTextValorOferta, currencyFormatter))

        if (relatorioId != null) {
            carregarRelatorioExistente(relatorioId!!)
            buttonEnviar.text = "Atualizar Relatório"
        } else {
            buttonEnviar.text = getString(R.string.form_button_enviar)
        }

        buttonEnviar.setOnClickListener {
            val dadosColetados = coletarDadosDoFormulario()
            salvarRelatorio(relatorioId, dadosColetados)
        }
    }

    private fun coletarDadosDoFormulario(): Map<String, Any> {
        val totalPessoas = editTextTotalPessoas.text.toString().toIntOrNull() ?: 0
        val totalVisitantes = editTextTotalVisitantes.text.toString().toIntOrNull() ?: 0

        val valorOfertaString = editTextValorOferta.text.toString()
        val valorOferta = try {
            currencyFormatter.parse(valorOfertaString)?.toDouble() ?: 0.0
        } catch (e: ParseException) {
            Log.e("FormularioRede", "Erro ao parsear valor da oferta: $valorOfertaString", e)
            0.0
        }

        val comentarios = editTextComentarios.text.toString().trim()
        val descricao = editTextDescricao.text.toString().trim()
        val nomeAutor = editTextNome.text.toString().trim()

        return hashMapOf(
            "totalPessoas" to totalPessoas,
            "totalVisitantes" to totalVisitantes,
            "valorOferta" to valorOferta,
            "comentarios" to comentarios,
            "descricao" to descricao,
            "autorNome" to nomeAutor
        )
    }

    private fun carregarRelatorioExistente(id: String) {
        firestore.collection("relatorios").document(id).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    editTextNome.setText(document.getString("autorNome") ?: "")
                    textViewData.text = document.getString("dataReuniao") ?: ""
                    editTextDescricao.setText(document.getString("descricao") ?: "")
                    editTextTotalPessoas.setText(document.getLong("totalPessoas")?.toString() ?: "")
                    editTextTotalVisitantes.setText(document.getLong("totalVisitantes")?.toString() ?: "")

                    val valorOfertaDouble = document.getDouble("valorOferta") ?: 0.0
                    editTextValorOferta.setText(currencyFormatter.format(valorOfertaDouble))

                    editTextComentarios.setText(document.getString("comentarios") ?: "")

                } else {
                    Toast.makeText(this, "Relatório não encontrado para edição.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao carregar relatório para edição: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("FormularioRede", "Erro ao carregar para edição", e)
                finish()
            }
    }

    private fun salvarRelatorio(relatorioIdParaSalvar: String?, relatorioDataMap: Map<String, Any>) {
        val collectionRef = firestore.collection("relatorios")

        val dataFinalReuniao: String = dataPendente ?: SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        val autorUid = auth.currentUser?.uid ?: ""
        val autorNome = editTextNome.text.toString().trim()
        val idRedeDoRelatorio = redeSelecionada!!

        val finalRelatorioData = relatorioDataMap.toMutableMap().apply {
            put("autorUid", autorUid)
            put("autorNome", autorNome)
            put("idRede", idRedeDoRelatorio)
            put("dataReuniao", dataFinalReuniao)
            put("timestamp", FieldValue.serverTimestamp())
        }

        val saveTask = if (relatorioIdParaSalvar != null) {
            collectionRef.document(relatorioIdParaSalvar).update(finalRelatorioData)
        } else {
            collectionRef.add(finalRelatorioData)
        }

        saveTask.addOnSuccessListener { taskResult ->
            // CORREÇÃO PARA ERRO 1 (Acessa o ID de forma mais defensiva)
            val savedDocId: String = if (relatorioIdParaSalvar != null) {
                relatorioIdParaSalvar
            } else {
                // Aqui, 'taskResult' é DocumentReference para add(), então podemos acessar 'id'
                (taskResult as? DocumentReference)?.id ?: "ID_DESCONHECIDO" // Use as? e Elvis operator para segurança
            }

            Toast.makeText(this, "Relatório salvo com sucesso! ID: $savedDocId", Toast.LENGTH_SHORT).show()
            Log.d("FormularioRede", "Relatório salvo: ID ${savedDocId}, Dados: $finalRelatorioData")
            finish()
        }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao salvar relatório: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("FormularioRede", "Erro ao salvar relatório", e)
            }
    }
}