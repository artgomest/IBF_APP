package com.ibf.app.ui.usuarios

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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

import com.ibf.app.R // Importação de R
import com.ibf.app.data.models.Perfil // Importação de Perfil, se for usado aqui
import com.ibf.app.ui.shared.SelecionarPerfilSheet // Importação da Sheet

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

    private fun configurarSpinnerDePapel() {
        val papeisPermitidos = getPapeisPermitidosParaCadastro(papelUsuarioLogado!!)
        if (papeisPermitidos.isEmpty()) {
            spinnerPapel.visibility = View.GONE
            findViewById<TextView>(R.id.text_selecionar_papel_label_cadastro)?.visibility = View.GONE
            buttonCadastrarUsuario.text = getString(R.string.sem_permissao_cadastro)
            buttonCadastrarUsuario.isEnabled = false
            return
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            papeisPermitidos
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPapel.adapter = adapter

        spinnerPapel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                buttonCadastrarUsuario.isEnabled = true
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                buttonCadastrarUsuario.isEnabled = false
            }
        }
        buttonCadastrarUsuario.isEnabled = false
    }

    private fun getPapeisPermitidosParaCadastro(papelLogado: String): List<String> {
        return when (papelLogado) {
            "pastor" -> listOf("lider", "secretario")
            "lider" -> listOf("secretario")
            else -> emptyList()
        }
    }

    private fun configurarBotaoCadastrar() {
        buttonCadastrarUsuario.setOnClickListener {
            val nome = editTextNome.text.toString().trim()
            val email = editTextEmail.text.toString().trim()
            val senha = editTextSenha.text.toString().trim()
            val papelSelecionado = spinnerPapel.selectedItem.toString()

            if (nome.isEmpty() || email.isEmpty() || senha.isEmpty()) {
                Toast.makeText(this, getString(R.string.preencher_todos_campos), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (senha.length < 6) {
                Toast.makeText(this, getString(R.string.senha_minimo_caracteres), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            cadastrarOuAtualizarUsuario(nome, email, senha, papelSelecionado, redeSelecionada!!)
        }
    }

    private fun cadastrarOuAtualizarUsuario(nome: String, email: String, senha: String, papel: String, rede: String) {
        buttonCadastrarUsuario.isEnabled = false

        auth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    user?.let {
                        salvarDadosNovoUsuarioNoFirestore(it.uid, nome, email, papel, rede)
                    }
                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthUserCollisionException) {
                        Log.d("CadastroUsuario", "Email $email já está em uso no Auth. Buscando no Firestore para atualizar funções.")
                        buscarUsuarioExistenteParaAtualizarFuncao(email, nome, papel, rede)
                    } else {
                        Toast.makeText(this, getString(R.string.falha_cadastro, exception?.message), Toast.LENGTH_LONG).show()
                        Log.e("CadastroUsuario", "Erro genérico ao criar usuário no Auth", exception)
                        buttonCadastrarUsuario.isEnabled = true
                    }
                }
            }
    }

    private fun salvarDadosNovoUsuarioNoFirestore(uid: String, nome: String, email: String, papel: String, rede: String) {
        val funcoesMap = hashMapOf(rede to papel)
        val userData = hashMapOf(
            "nome" to nome,
            "email" to email,
            "funcoes" to funcoesMap,
            "redes" to listOf(rede)
        )

        firestore.collection("usuarios").document(uid).set(userData)
            .addOnSuccessListener {
                Toast.makeText(this, getString(R.string.usuario_cadastrado_sucesso, nome, papel, rede), Toast.LENGTH_LONG).show()
                limparCampos()
                buttonCadastrarUsuario.isEnabled = true
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, getString(R.string.falha_salvar_dados_firestore, e.message), Toast.LENGTH_SHORT).show()
                Log.e("CadastroUsuario", "Erro ao salvar usuário no Firestore", e)
                buttonCadastrarUsuario.isEnabled = true
            }
    }

    private fun buscarUsuarioExistenteParaAtualizarFuncao(email: String, nomeFormulario: String, novoPapel: String, novaRede: String) {
        firestore.collection("usuarios")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val existingDoc = querySnapshot.documents.first()
                    val existingUserUid = existingDoc.id
                    val existingUserName = existingDoc.getString("nome") ?: getString(R.string.usuario_existente_label)
                    @Suppress("UNCHECKED_CAST")
                    val existingFuncoes = existingDoc.get("funcoes") as? HashMap<String, String>

                    if (existingFuncoes != null && existingFuncoes.containsKey(novaRede)) {
                        val papelExistente = existingFuncoes[novaRede]
                        Toast.makeText(
                            this,
                            getString(R.string.email_ja_na_rede, email, existingUserName, papelExistente, novaRede),
                            Toast.LENGTH_LONG
                        ).show()
                        limparCampos()
                        buttonCadastrarUsuario.isEnabled = true
                    } else {
                        mostrarDialogoAdicionarFuncao(existingUserUid, nomeFormulario, email, novoPapel, novaRede)
                    }
                } else {
                    Toast.makeText(this, getString(R.string.erro_email_cadastrado_sem_dados), Toast.LENGTH_LONG).show()
                    Log.e("CadastroUsuario", "Email $email existe no Auth, mas documento não no Firestore.")
                    limparCampos()
                    buttonCadastrarUsuario.isEnabled = true
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, getString(R.string.erro_buscar_usuario_existente, e.message), Toast.LENGTH_SHORT).show()
                Log.e("CadastroUsuario", "Erro no Firestore ao buscar usuário existente", e)
                buttonCadastrarUsuario.isEnabled = true
            }
    }

    private fun mostrarDialogoAdicionarFuncao(
        existingUserUid: String,
        existingUserName: String,
        email: String,
        novoPapel: String,
        novaRede: String
    ) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialogo_usuario_existente_titulo))
            .setMessage(getString(R.string.dialogo_adicionar_funcao_mensagem, email, existingUserName, novoPapel, novaRede))
            .setPositiveButton(getString(R.string.sim_adicionar_funcao)) { dialog, _ ->
                adicionarFuncaoAUsuarioExistente(existingUserUid, novoPapel, novaRede, existingUserName)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.nao_cancelar)) { dialog, _ ->
                Toast.makeText(this, getString(R.string.operacao_cancelada), Toast.LENGTH_SHORT).show()
                limparCampos()
                buttonCadastrarUsuario.isEnabled = true
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun adicionarFuncaoAUsuarioExistente(uid: String, novoPapel: String, novaRede: String, nomeParaAtualizar: String) {
        val userRef = firestore.collection("usuarios").document(uid)

        val updates = hashMapOf<String, Any>(
            "funcoes.$novaRede" to novoPapel,
            "redes" to FieldValue.arrayUnion(novaRede)
        )
        // Opcional: Se o nome digitado no formulário for diferente do nome existente no Firestore,
        // você pode atualizar o nome do usuário também.
        // updates["nome"] = nomeParaAtualizar

        userRef.update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, getString(R.string.funcao_adicionada_sucesso, novoPapel, novaRede, nomeParaAtualizar), Toast.LENGTH_LONG).show()
                limparCampos()
                buttonCadastrarUsuario.isEnabled = true
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, getString(R.string.erro_adicionar_funcao, e.message), Toast.LENGTH_SHORT).show()
                Log.e("CadastroUsuario", "Erro ao adicionar função ao usuário existente", e)
                buttonCadastrarUsuario.isEnabled = true
            }
    }

    private fun limparCampos() {
        editTextNome.text?.clear()
        editTextEmail.text?.clear()
        editTextSenha.text?.clear()
        spinnerPapel.setSelection(0)
        buttonCadastrarUsuario.isEnabled = false
    }
}