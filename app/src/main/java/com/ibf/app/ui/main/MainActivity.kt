package com.ibf.app.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ibf.app.R
import com.ibf.app.ui.dashboard.LiderDashboardActivity
import com.ibf.app.ui.dashboard.PastorDashboardActivity
import com.ibf.app.ui.dashboard.SecretarioDashboardActivity
import com.ibf.app.ui.shared.SelecionarPerfilSheet
import android.widget.TextView
import com.ibf.app.ui.usuarios.SolicitacaoCadastroActivity

class MainActivity : AppCompatActivity(), SelecionarPerfilSheet.PerfilSelecionadoListener {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var textCadastreSe: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        emailInput = findViewById(R.id.editTextEmailAddress)
        passwordInput = findViewById(R.id.editTextPassword)
        loginButton = findViewById(R.id.buttonLogin)
        textCadastreSe = findViewById(R.id.text_cadastre_se)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.preencher_todos_campos), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }



            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        processarLogin()
                    } else {
                        Toast.makeText(this, getString(R.string.falha_login_credenciais), Toast.LENGTH_SHORT).show()
                    }
                }
        }
        textCadastreSe.setOnClickListener {
            val intent = Intent(this, SolicitacaoCadastroActivity::class.java)
            startActivity(intent)
        }
    }

    private fun processarLogin() {
        val user = firebaseAuth.currentUser ?: return

        firestore.collection("usuarios").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nomeUsuario = document.getString("nome") ?: getString(R.string.usuario_padrao)
                    @Suppress("UNCHECKED_CAST")
                    val funcoes = document.get("funcoes") as? HashMap<String, String>

                    if (funcoes.isNullOrEmpty()) {
                        Toast.makeText(this, getString(R.string.usuario_sem_papeis_definidos), Toast.LENGTH_LONG).show()
                        firebaseAuth.signOut()
                        return@addOnSuccessListener
                    }

                    if (funcoes.size > 1) {
                        val bottomSheet = SelecionarPerfilSheet.newInstance(funcoes, nomeUsuario)
                        bottomSheet.show(supportFragmentManager, "SelecionarPerfilSheet")
                    } else {
                        val (rede, papel) = funcoes.entries.first()
                        salvarRedeSelecionada(rede)
                        navegarParaTelaCorreta(rede, papel)
                    }
                } else {
                    Toast.makeText(this, getString(R.string.dados_usuario_nao_encontrados), Toast.LENGTH_LONG).show()
                    firebaseAuth.signOut()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, getString(R.string.erro_buscar_dados, exception.message), Toast.LENGTH_SHORT).show()
                Log.e("LOGIN_ERROR", "Erro ao buscar documento do usuário", exception)
            }
    }

    override fun onPerfilSelecionado(rede: String, papel: String) {
        Toast.makeText(this, getString(R.string.entrando_como_papel_rede, papel, rede), Toast.LENGTH_SHORT).show()
        salvarRedeSelecionada(rede)
        navegarParaTelaCorreta(rede, papel)
    }

    override fun onLogoutSelecionado() {
        // Na tela de login, o logout não faz nada
    }

    private fun salvarRedeSelecionada(rede: String) {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPref.edit {
            putString("REDE_SELECIONADA", rede)
        }
    }

    private fun navegarParaTelaCorreta(rede: String, papel: String?) {
        val intent = when (papel) {
            "pastor" -> Intent(this, PastorDashboardActivity::class.java)
            "lider" -> Intent(this, LiderDashboardActivity::class.java)
            "secretario" -> Intent(this, SecretarioDashboardActivity::class.java)
            else -> null
        }

        if (intent != null) {
            intent.putExtra("REDE_SELECIONADA", rede)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, getString(R.string.papel_desconhecido, papel), Toast.LENGTH_LONG).show()
            firebaseAuth.signOut()
        }
    }
}