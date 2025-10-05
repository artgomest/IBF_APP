package com.ibf.app.ui.usuarios

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.ibf.app.R

class FichaMembroActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private var membroId: String? = null
    private var isEditing = false

    // Declaração dos componentes da UI
    private lateinit var textPageTitle: TextView
    private lateinit var textNomeMembro: TextInputEditText
    private lateinit var textDataNascimento: TextInputEditText
    private lateinit var fabEditarMembro: FloatingActionButton
    private lateinit var fabSalvarMembro: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ficha_membro)

        firestore = FirebaseFirestore.getInstance()
        membroId = intent.getStringExtra("MEMBRO_ID")

        // Inicializa os componentes da UI
        textPageTitle = findViewById(R.id.text_page_title)
        textNomeMembro = findViewById(R.id.text_nome_membro)
        textDataNascimento = findViewById(R.id.text_data_nascimento)
        fabEditarMembro = findViewById(R.id.fab_editar_membro)
        fabSalvarMembro = findViewById(R.id.fab_salvar_membro)

        if (membroId == null) {
            Toast.makeText(this, "Erro: ID do membro não fornecido.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        configurarBotoes()
        carregarDadosDoMembro()
    }

    private fun configurarBotoes() {
        findViewById<ImageView>(R.id.button_back).setOnClickListener { finish() }

        fabEditarMembro.setOnClickListener {
            alternarModoEdicao(true)
        }

        fabSalvarMembro.setOnClickListener {
            salvarAlteracoes()
        }
    }

    private fun alternarModoEdicao(editar: Boolean) {
        isEditing = editar
        textNomeMembro.isEnabled = editar
        textDataNascimento.isEnabled = editar

        if (editar) {
            fabEditarMembro.visibility = View.GONE
            fabSalvarMembro.visibility = View.VISIBLE
            textNomeMembro.requestFocus() // Foca no campo de nome para facilitar
        } else {
            fabEditarMembro.visibility = View.VISIBLE
            fabSalvarMembro.visibility = View.GONE
        }
    }

    private fun salvarAlteracoes() {
        val novoNome = textNomeMembro.text.toString().trim()
        val novaDataNascimento = textDataNascimento.text.toString().trim()

        if (novoNome.isEmpty()) {
            textNomeMembro.error = "O nome não pode ficar em branco."
            return
        }

        val dadosAtualizados = hashMapOf<String, Any>(
            "nome" to novoNome,
            "dataNascimento" to novaDataNascimento
        )

        membroId?.let { id ->
            firestore.collection("usuarios").document(id)
                .update(dadosAtualizados)
                .addOnSuccessListener {
                    Toast.makeText(this, "Dados atualizados com sucesso!", Toast.LENGTH_SHORT).show()
                    alternarModoEdicao(false) // Sai do modo de edição
                    textPageTitle.text = novoNome // Atualiza o título da página
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Falha ao atualizar os dados.", Toast.LENGTH_SHORT).show()
                    Log.e("FichaMembroActivity", "Erro ao salvar: ${e.message}", e)
                }
        }
    }

    private fun carregarDadosDoMembro() {
        membroId?.let { id ->
            firestore.collection("usuarios").document(id).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val nome = document.getString("nome") ?: "Nome não encontrado"
                        val dataNascimento = document.getString("dataNascimento") ?: ""

                        textPageTitle.text = nome
                        textNomeMembro.setText(nome)
                        textDataNascimento.setText(dataNascimento)

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
}

