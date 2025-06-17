package com.ibf.app.ui.usuarios

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.ibf.app.R

class DetalhesUsuarioActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var editTextNome: TextInputEditText
    private lateinit var editTextEmail: TextInputEditText
    private lateinit var editTextPapel: TextInputEditText
    private lateinit var layoutPapelTexto: TextInputLayout
    private lateinit var spinnerPapel: Spinner
    private lateinit var buttonEditarSalvar: Button
    private lateinit var buttonRemover: Button

    private var usuarioId: String? = null
    private var redeId: String? = null
    private var papelUsuarioLogado: String? = null

    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhes_usuario)

        firestore = FirebaseFirestore.getInstance()

        usuarioId = intent.getStringExtra("USUARIO_ID")
        redeId = intent.getStringExtra("REDE_ID")
        papelUsuarioLogado = intent.getStringExtra("PAPEL_USUARIO_LOGADO")

        if (usuarioId == null || redeId == null) {
            Toast.makeText(this, "Erro: Informações do usuário ou da rede não encontradas.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        editTextNome = findViewById(R.id.edit_text_nome_detalhes)
        editTextEmail = findViewById(R.id.edit_text_email_detalhes)
        editTextPapel = findViewById(R.id.edit_text_papel_detalhes)
        layoutPapelTexto = findViewById(R.id.layout_papel_texto)
        spinnerPapel = findViewById(R.id.spinner_papel_detalhes)
        buttonEditarSalvar = findViewById(R.id.button_editar_salvar)
        buttonRemover = findViewById(R.id.button_remover_usuario)

        findViewById<ImageView>(R.id.button_back).setOnClickListener { finish() }

        carregarDadosUsuario()

        buttonEditarSalvar.setOnClickListener {
            if (isEditMode) {
                salvarAlteracoes()
            } else {
                entrarModoEdicao()
            }
        }

        buttonRemover.setOnClickListener {
            confirmarRemocao()
        }
    }

    private fun carregarDadosUsuario() {
        firestore.collection("usuarios").document(usuarioId!!)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nome = document.getString("nome") ?: ""
                    val email = document.getString("email") ?: ""
                    @Suppress("UNCHECKED_CAST")
                    val funcoes = document.get("funcoes") as? Map<String, String>
                    val papelNaRede = funcoes?.get(redeId!!)?.replaceFirstChar { it.titlecase() } ?: "Papel não definido"
                    editTextNome.setText(nome)
                    editTextEmail.setText(email)
                    editTextPapel.setText(papelNaRede)
                } else {
                    Toast.makeText(this, "Usuário não encontrado.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao carregar dados: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("DetalhesUsuario", "Erro ao buscar documento", e)
                finish()
            }
    }

    private fun entrarModoEdicao() {
        isEditMode = true
        buttonEditarSalvar.text = getString(R.string.salvar_alteracoes)
        buttonRemover.visibility = View.GONE

        editTextNome.isEnabled = true
        layoutPapelTexto.visibility = View.GONE
        spinnerPapel.visibility = View.VISIBLE

        configurarSpinnerDePapel()
    }

    private fun salvarAlteracoes() {
        val novoNome = editTextNome.text.toString().trim()
        val novoPapel = spinnerPapel.selectedItem.toString().lowercase()

        val updates = hashMapOf<String, Any>(
            "nome" to novoNome,
            "funcoes.$redeId" to novoPapel
        )

        firestore.collection("usuarios").document(usuarioId!!)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Usuário atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                sairModoEdicao()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Falha ao atualizar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sairModoEdicao() {
        isEditMode = false
        buttonEditarSalvar.text = getString(R.string.editar_informacoes)
        buttonRemover.visibility = View.VISIBLE

        editTextNome.isEnabled = false
        layoutPapelTexto.visibility = View.VISIBLE
        spinnerPapel.visibility = View.GONE

        carregarDadosUsuario()
    }

    private fun confirmarRemocao() {
        AlertDialog.Builder(this)
            .setTitle("Remover da Rede")
            .setMessage("Tem certeza que deseja remover ${editTextNome.text} da rede '$redeId'? O acesso dele(a) a esta rede será revogado.")
            .setPositiveButton("Sim, Remover") { _, _ ->
                removerUsuarioDaRede()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun removerUsuarioDaRede() {
        val userRef = firestore.collection("usuarios").document(usuarioId!!)
        val updates = hashMapOf<String, Any>(
            "funcoes.$redeId" to FieldValue.delete(),
            "redes" to FieldValue.arrayRemove(redeId!!)
        )

        userRef.update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Usuário removido da rede com sucesso.", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Falha ao remover usuário: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("DetalhesUsuario", "Erro ao remover usuário da rede", e)
            }
    }

    private fun configurarSpinnerDePapel() {
        val papeisPermitidos = when (papelUsuarioLogado) {
            "pastor" -> listOf("Lider", "Secretario")
            "lider" -> listOf("Secretario")
            else -> emptyList()
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, papeisPermitidos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPapel.adapter = adapter
    }
}