package com.ibf.app.ui.usuarios

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.ibf.app.R

class CadastroUsuarioActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var textRedeCadastro: TextView
    private lateinit var editTextNome: TextInputEditText
    private lateinit var editTextEmail: TextInputEditText
    private lateinit var editTextSenha: TextInputEditText
    private lateinit var spinnerPapel: Spinner
    private lateinit var buttonCadastrarUsuario: Button

    private var redeSelecionada: String? = null
    private var papelUsuarioLogado: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro_usuario)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        findViewById<TextView>(R.id.text_page_title).text = getString(R.string.cadastro_de_usuario)
        findViewById<ImageView>(R.id.button_back).setOnClickListener { finish() }

        textRedeCadastro = findViewById(R.id.text_rede_cadastro)
        editTextNome = findViewById(R.id.edit_text_nome)
        editTextEmail = findViewById(R.id.edit_text_email)
        editTextSenha = findViewById(R.id.edit_text_senha)
        spinnerPapel = findViewById(R.id.spinner_papel_cadastro)
        buttonCadastrarUsuario = findViewById(R.id.button_cadastrar_usuario_final)

        redeSelecionada = intent.getStringExtra("REDE_SELECIONADA")
        papelUsuarioLogado = intent.getStringExtra("PAPEL_USUARIO_LOGADO")

        if (redeSelecionada == null || papelUsuarioLogado == null) {
            Toast.makeText(this, getString(R.string.erro_rede_ou_papel_nao_especificados), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        textRedeCadastro.text = getString(R.string.cadastrando_na_rede_label, redeSelecionada)

        configurarSpinnerDePapel()
        configurarBotaoCadastrar()
    }

    private fun configurarBotaoCadastrar() {
        buttonCadastrarUsuario.setOnClickListener {
            val nome = editTextNome.text.toString().trim()
            val email = editTextEmail.text.toString().trim()
            val senha = editTextSenha.text.toString().trim()

            // Verifica se há algo selecionado no spinner antes de prosseguir
            if (spinnerPapel.selectedItem == null) {
                Toast.makeText(this, "Por favor, selecione um papel.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val papelSelecionado = spinnerPapel.selectedItem.toString().lowercase()

            if (nome.isEmpty() || email.isEmpty() || senha.isEmpty()) {
                Toast.makeText(this, getString(R.string.preencher_todos_campos), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (senha.length < 6) {
                Toast.makeText(this, getString(R.string.senha_minimo_caracteres), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            cadastrarOuAtualizarUsuarioLocal(nome, email, senha, papelSelecionado, redeSelecionada!!)
        }
    }

    private fun configurarSpinnerDePapel() {
        val papeisPermitidos = getPapeisPermitidosParaCadastro(papelUsuarioLogado!!)
        if (papeisPermitidos.isEmpty()) {
            spinnerPapel.visibility = View.GONE
            findViewById<TextView>(R.id.text_selecionar_papel_label_cadastro)?.visibility = View.GONE
            buttonCadastrarUsuario.text = getString(R.string.sem_permissao_cadastro)
            buttonCadastrarUsuario.isEnabled = false
            return
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, papeisPermitidos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPapel.adapter = adapter

        // CORREÇÃO: Habilita o botão imediatamente, já que sabemos que há papéis para selecionar.
        buttonCadastrarUsuario.isEnabled = true

        spinnerPapel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Não precisa mais habilitar o botão aqui.
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Se por algum motivo o spinner ficar sem seleção, desabilita o botão.
                buttonCadastrarUsuario.isEnabled = false
            }
        }
    }

    private fun getPapeisPermitidosParaCadastro(papelLogado: String): List<String> {
        return when (papelLogado) {
            "pastor" -> listOf("Lider", "Secretario")
            "lider" -> listOf("Secretario")
            else -> emptyList()
        }
    }

    private fun cadastrarOuAtualizarUsuarioLocal(nome: String, email: String, senha: String, papel: String, rede: String) {
        buttonCadastrarUsuario.isEnabled = false
        val tempAuth = FirebaseAuth.getInstance()

        tempAuth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    task.result?.user?.let { newUser ->
                        salvarDadosNovoUsuarioNoFirestore(newUser, nome, email, papel, rede)
                        tempAuth.signOut()
                    }
                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthUserCollisionException) {
                        buscarUsuarioExistenteParaAtualizarFuncao(email, nome, papel, rede)
                    } else {
                        Toast.makeText(this, getString(R.string.falha_cadastro, exception?.message), Toast.LENGTH_LONG).show()
                        buttonCadastrarUsuario.isEnabled = true
                    }
                }
            }
    }

    private fun salvarDadosNovoUsuarioNoFirestore(newUser: FirebaseUser, nome: String, email: String, papel: String, rede: String) {
        val funcoesMap = hashMapOf(rede to papel)
        val userData = hashMapOf(
            "nome" to nome, "email" to email, "funcoes" to funcoesMap, "redes" to listOf(rede)
        )
        firestore.collection("usuarios").document(newUser.uid).set(userData)
            .addOnSuccessListener {
                Toast.makeText(this, getString(R.string.usuario_cadastrado_sucesso, nome, papel, rede), Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, getString(R.string.falha_salvar_dados_firestore, e.message), Toast.LENGTH_SHORT).show()
                newUser.delete()
                buttonCadastrarUsuario.isEnabled = true
            }
    }

    private fun buscarUsuarioExistenteParaAtualizarFuncao(email: String, nomeFormulario: String, novoPapel: String, novaRede: String) {
        firestore.collection("usuarios").whereEqualTo("email", email).limit(1).get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val doc = querySnapshot.documents.first()
                    @Suppress("UNCHECKED_CAST")
                    val funcoes = doc.get("funcoes") as? HashMap<String, String>
                    if (funcoes != null && funcoes.containsKey(novaRede)) {
                        Toast.makeText(this, "Este usuário já possui um papel nesta rede.", Toast.LENGTH_LONG).show()
                        buttonCadastrarUsuario.isEnabled = true
                    } else {
                        mostrarDialogoAdicionarFuncao(doc.id, doc.getString("nome") ?: nomeFormulario, email, novoPapel, novaRede)
                    }
                } else {
                    Toast.makeText(this, getString(R.string.erro_email_cadastrado_sem_dados), Toast.LENGTH_LONG).show()
                    buttonCadastrarUsuario.isEnabled = true
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.erro_buscar_dados), Toast.LENGTH_SHORT).show()
                buttonCadastrarUsuario.isEnabled = true
            }
    }

    private fun mostrarDialogoAdicionarFuncao(uid: String, nome: String, email: String, novoPapel: String, novaRede: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialogo_usuario_existente_titulo))
            .setMessage(getString(R.string.dialogo_adicionar_funcao_mensagem, email, nome, novoPapel, novaRede))
            .setPositiveButton(getString(R.string.sim_adicionar_funcao)) { _, _ ->
                adicionarFuncaoAUsuarioExistente(uid, novoPapel, novaRede, nome)
            }
            .setNegativeButton(getString(R.string.nao_cancelar)) { _, _ ->
                buttonCadastrarUsuario.isEnabled = true
            }
            .show()
    }

    private fun adicionarFuncaoAUsuarioExistente(uid: String, novoPapel: String, novaRede: String, nome: String) {
        val updates = hashMapOf<String, Any>(
            "funcoes.$novaRede" to novoPapel, "redes" to FieldValue.arrayUnion(novaRede)
        )
        firestore.collection("usuarios").document(uid).update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, getString(R.string.funcao_adicionada_sucesso, novoPapel, novaRede, nome), Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, getString(R.string.erro_adicionar_funcao, e.message), Toast.LENGTH_SHORT).show()
                buttonCadastrarUsuario.isEnabled = true
            }
    }
}