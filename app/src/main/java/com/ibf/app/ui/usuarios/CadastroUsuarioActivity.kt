// Em app/src/main/java/com/ibf/app/ui/usuarios/CadastroUsuarioActivity.kt

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
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.ibf.app.R
import com.ibf.app.data.models.SolicitacaoCadastro

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

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, papeisPermitidos)
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

            criarUsuarioPendente(nome, email, senha, papelSelecionado, redeSelecionada!!)
        }
    }

    /**
     * NOVA LÓGICA: Cria o usuário no Auth, mas gera uma solicitação pendente no Firestore.
     */
    private fun criarUsuarioPendente(nome: String, email: String, senha: String, papel: String, rede: String) {
        buttonCadastrarUsuario.isEnabled = false
        val solicitanteUid = auth.currentUser?.uid
        if (solicitanteUid == null) {
            Toast.makeText(this, "Erro: Usuário solicitante não está logado.", Toast.LENGTH_LONG).show()
            buttonCadastrarUsuario.isEnabled = true
            return
        }

        // Tenta criar o usuário no Firebase Authentication
        auth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Usuário criado no Auth com sucesso!
                    val newUser = task.result?.user
                    newUser?.let {
                        salvarSolicitacaoDeCadastro(it, nome, email, papel, rede, solicitanteUid)
                    }
                } else {
                    // Trata falhas, como e-mail já existente
                    val exception = task.exception
                    if (exception is FirebaseAuthUserCollisionException) {
                        Toast.makeText(this, "Este e-mail já possui uma conta.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, getString(R.string.falha_cadastro, exception?.message), Toast.LENGTH_LONG).show()
                    }
                    Log.e("CadastroUsuario", "Erro ao criar usuário no Auth", exception)
                    buttonCadastrarUsuario.isEnabled = true
                }
            }
    }

    /**
     * NOVA FUNÇÃO: Salva um documento na coleção 'solicitacoesCadastro'.
     */
    private fun salvarSolicitacaoDeCadastro(newUser: FirebaseUser, nome: String, email: String, papel: String, rede: String, solicitanteUid: String) {
        val solicitacao = SolicitacaoCadastro(
            uid = newUser.uid,
            nome = nome,
            email = email,
            redeId = rede,
            papelSolicitado = papel,
            status = "pendente",
            solicitadoPorUid = solicitanteUid
        )

        firestore.collection("solicitacoesCadastro").add(solicitacao)
            .addOnSuccessListener {
                // SUCESSO! A solicitação foi enviada.
                Toast.makeText(this, "Solicitação de cadastro enviada para aprovação.", Toast.LENGTH_LONG).show()
                limparCampos()
                // A Activity é finalizada e o líder volta para a tela anterior.
                // Não há necessidade de deslogar/relogar aqui, pois a sessão do líder continua ativa.
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Falha ao enviar solicitação: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("CadastroUsuario", "Erro ao salvar solicitação no Firestore", e)
                // Se falhar ao salvar a solicitação, devemos apagar o usuário criado no Auth para não deixar lixo.
                newUser.delete()
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