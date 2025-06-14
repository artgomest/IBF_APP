package com.ibf.app

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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

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
            Toast.makeText(this, "Erro: Rede ou Papel do usuário não especificados.", Toast.LENGTH_LONG).show()
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
                Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (senha.length < 6) {
                Toast.makeText(this, "A senha deve ter no mínimo 6 caracteres.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // AQUI ESTÁ A MUDANÇA: Tentamos cadastrar diretamente.
            // Se o email já estiver em uso, a exceção será capturada.
            cadastrarOuAtualizarUsuario(nome, email, senha, papelSelecionado, redeSelecionada!!)
        }
    }

    // FUNÇÃO REESTRUTURADA: Tenta cadastrar ou captura o erro para atualizar
    private fun cadastrarOuAtualizarUsuario(nome: String, email: String, senha: String, papel: String, rede: String) {
        buttonCadastrarUsuario.isEnabled = false // Desabilita o botão

        // Tenta criar o usuário no Firebase Authentication
        auth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Usuário criado com sucesso -> Salvar dados no Firestore
                    val user = task.result?.user
                    user?.let {
                        salvarDadosNovoUsuarioNoFirestore(it.uid, nome, email, papel, rede)
                    }
                } else {
                    // Falha na criação do usuário
                    val exception = task.exception
                    if (exception is FirebaseAuthUserCollisionException) {
                        // E-mail já em uso! -> Prosseguir com a lógica de atualização
                        Log.d("CadastroUsuario", "Email $email já está em uso, buscando no Firestore para atualizar funções.")
                        buscarUsuarioExistenteParaAtualizarFuncao(email, nome, papel, rede)
                    } else {
                        // Outro tipo de erro de cadastro
                        Toast.makeText(this, "Falha no cadastro: ${exception?.message}", Toast.LENGTH_LONG).show()
                        Log.e("CadastroUsuario", "Erro genérico ao criar usuário no Auth", exception)
                        buttonCadastrarUsuario.isEnabled = true // Reabilita o botão
                    }
                }
            }
    }

    // NOVA FUNÇÃO: Salva dados de um NOVO usuário no Firestore
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
                Toast.makeText(this, "Usuário $nome cadastrado como $papel na $rede!", Toast.LENGTH_LONG).show()
                limparCampos()
                buttonCadastrarUsuario.isEnabled = true
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Falha ao salvar dados no Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("CadastroUsuario", "Erro ao salvar usuário no Firestore", e)
                // Opcional: tentar deletar o usuário do Auth se a gravação no Firestore falhou
                auth.currentUser?.delete()?.addOnCompleteListener { deleteTask ->
                    if (deleteTask.isSuccessful) Log.d("CadastroUsuario", "Usuário do Auth excluído após falha no Firestore.")
                }
                buttonCadastrarUsuario.isEnabled = true
            }
    }


    // NOVA FUNÇÃO: Busca usuário existente pelo email para atualização de função
    private fun buscarUsuarioExistenteParaAtualizarFuncao(email: String, novoNome: String, novoPapel: String, novaRede: String) {
        firestore.collection("usuarios")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val existingDoc = querySnapshot.documents.first()
                    val existingUserUid = existingDoc.id
                    val existingUserName = existingDoc.getString("nome") ?: "Usuário Existente"
                    @Suppress("UNCHECKED_CAST")
                    val existingFuncoes = existingDoc.get("funcoes") as? HashMap<String, String>

                    if (existingFuncoes != null && existingFuncoes.containsKey(novaRede)) {
                        // Usuário já tem acesso a ESTA REDE com a função.
                        val papelExistente = existingFuncoes[novaRede]
                        Toast.makeText(
                            this,
                            "'$email' ($existingUserName) já é $papelExistente na $novaRede.",
                            Toast.LENGTH_LONG
                        ).show()
                        limparCampos()
                        buttonCadastrarUsuario.isEnabled = true
                    } else {
                        // E-mail existe, mas o usuário NÃO TEM acesso a esta rede.
                        // Perguntar ao líder se quer adicionar nova função.
                        mostrarDialogoAdicionarFuncao(existingUserUid, existingUserName, email, novoPapel, novaRede)
                    }
                } else {
                    // E-mail existe no Auth, mas não encontrou o documento no Firestore.
                    // Isso pode indicar um erro de sincronização ou usuário antigo.
                    Toast.makeText(this, "Erro: Email cadastrado no Auth, mas dados não encontrados no Firestore. Contacte suporte.", Toast.LENGTH_LONG).show()
                    Log.e("CadastroUsuario", "Email $email existe no Auth, mas documento não no Firestore.")
                    limparCampos()
                    buttonCadastrarUsuario.isEnabled = true
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao buscar usuário existente no Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("CadastroUsuario", "Erro no Firestore ao buscar usuário existente", e)
                buttonCadastrarUsuario.isEnabled = true
            }
    }

    // ... (restante das funções: mostrarDialogoAdicionarFuncao, adicionarFuncaoAUsuarioExistente, limparCampos) ...

    private fun mostrarDialogoAdicionarFuncao(
        existingUserUid: String,
        existingUserName: String,
        email: String,
        novoPapel: String,
        novaRede: String
    ) {
        AlertDialog.Builder(this)
            .setTitle("Usuário Existente")
            .setMessage("O email '$email' ($existingUserName) já está cadastrado. Deseja adicionar o papel '$novoPapel' na '$novaRede' para este usuário?")
            .setPositiveButton("Sim, Adicionar Função") { dialog, _ ->
                adicionarFuncaoAUsuarioExistente(existingUserUid, novoPapel, novaRede)
                dialog.dismiss()
            }
            .setNegativeButton("Não, Cancelar") { dialog, _ ->
                Toast.makeText(this, "Operação cancelada.", Toast.LENGTH_SHORT).show()
                limparCampos()
                buttonCadastrarUsuario.isEnabled = true
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun adicionarFuncaoAUsuarioExistente(uid: String, novoPapel: String, novaRede: String) {
        val userRef = firestore.collection("usuarios").document(uid)

        // Atualiza o mapa 'funcoes' e o array 'redes'
        userRef.update(
            "funcoes.$novaRede", novoPapel,
            "redes", FieldValue.arrayUnion(novaRede)
        )
            .addOnSuccessListener {
                Toast.makeText(this, "Função '$novoPapel' na '$novaRede' adicionada ao usuário!", Toast.LENGTH_LONG).show()
                limparCampos()
                buttonCadastrarUsuario.isEnabled = true
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao adicionar função: ${e.message}", Toast.LENGTH_SHORT).show()
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