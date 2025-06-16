// Em app/src/main/java/com/ibf/app/ui/usuarios/SolicitacaoCadastroActivity.kt

package com.ibf.app.ui.usuarios

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.ibf.app.R
import com.ibf.app.data.models.SolicitacaoCadastro

class SolicitacaoCadastroActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var editTextNome: TextInputEditText
    private lateinit var editTextEmail: TextInputEditText
    private lateinit var editTextSenha: TextInputEditText
    private lateinit var spinnerRedes: Spinner
    private lateinit var buttonEnviarSolicitacao: Button

    private val listaNomesRedes = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_solicitacao_cadastro)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        findViewById<ImageView>(R.id.button_back).setOnClickListener { finish() }
        editTextNome = findViewById(R.id.edit_text_nome)
        editTextEmail = findViewById(R.id.edit_text_email)
        editTextSenha = findViewById(R.id.edit_text_senha)
        spinnerRedes = findViewById(R.id.spinner_redes)
        buttonEnviarSolicitacao = findViewById(R.id.button_enviar_solicitacao)

        carregarRedesParaSpinner()
        configurarBotaoEnviar()
    }

    private fun carregarRedesParaSpinner() {
        firestore.collection("redes").get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Nenhuma rede disponível para cadastro.", Toast.LENGTH_LONG).show()
                    buttonEnviarSolicitacao.isEnabled = false
                    return@addOnSuccessListener
                }
                for (document in documents) {
                    val nomeRede = document.getString("nome")
                    if (nomeRede != null) {
                        listaNomesRedes.add(nomeRede)
                    }
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listaNomesRedes)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerRedes.adapter = adapter
            }
            .addOnFailureListener { e ->
                Log.e("SolicitacaoCadastro", "Erro ao buscar redes", e)
                Toast.makeText(this, "Erro ao carregar redes. Tente novamente.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun configurarBotaoEnviar() {
        buttonEnviarSolicitacao.setOnClickListener {
            val nome = editTextNome.text.toString().trim()
            val email = editTextEmail.text.toString().trim()
            val senha = editTextSenha.text.toString().trim()

            if (nome.isEmpty() || email.isEmpty() || senha.isEmpty() || spinnerRedes.selectedItem == null) {
                Toast.makeText(this, getString(R.string.preencher_todos_campos), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (senha.length < 6) {
                Toast.makeText(this, getString(R.string.senha_minimo_caracteres), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val redeSelecionada = spinnerRedes.selectedItem.toString()
            criarSolicitacaoDeUsuario(nome, email, senha, redeSelecionada)
        }
    }

    private fun criarSolicitacaoDeUsuario(nome: String, email: String, senha: String, rede: String) {
        buttonEnviarSolicitacao.isEnabled = false

        auth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val newUser = task.result?.user
                    newUser?.let {
                        // O UID do solicitante é o próprio UID do novo usuário, pois é um auto-cadastro
                        salvarSolicitacao(it, nome, email, rede, it.uid)
                    }
                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthUserCollisionException) {
                        Toast.makeText(this, "Este e-mail já está em uso.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, getString(R.string.falha_cadastro, exception?.message), Toast.LENGTH_LONG).show()
                    }
                    buttonEnviarSolicitacao.isEnabled = true
                }
            }
    }

    private fun salvarSolicitacao(newUser: FirebaseUser, nome: String, email: String, rede: String, solicitanteUid: String) {
        val solicitacao = SolicitacaoCadastro(
            uid = newUser.uid,
            nome = nome,
            email = email,
            redeId = rede,
            papelSolicitado = "secretario", // Papel padrão para cadastro público
            status = "pendente",
            solicitadoPorUid = solicitanteUid
        )

        firestore.collection("solicitacoesCadastro").add(solicitacao)
            .addOnSuccessListener {
                Toast.makeText(this, getString(R.string.solicitacao_enviada_sucesso), Toast.LENGTH_LONG).show()
                auth.signOut() // Desloga a sessão temporária criada
                finish() // Volta para a tela de login
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Falha ao enviar solicitação: ${e.message}", Toast.LENGTH_LONG).show()
                newUser.delete() // Limpa o usuário do Auth se a solicitação falhar
                buttonEnviarSolicitacao.isEnabled = true
            }
    }
}