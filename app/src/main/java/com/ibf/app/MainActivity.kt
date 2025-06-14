package com.ibf.app

import android.content.Context // Importação necessária
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.Serializable

class MainActivity : AppCompatActivity(), SelecionarPerfilSheet.PerfilSelecionadoListener {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("FLUXO_APP", "MainActivity: onCreate")
        setContentView(R.layout.activity_main)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        emailInput = findViewById(R.id.editTextEmailAddress)
        passwordInput = findViewById(R.id.editTextPassword)
        loginButton = findViewById(R.id.buttonLogin)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        processarLogin()
                    } else {
                        Toast.makeText(this, "Falha no login. Verifique suas credenciais.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun processarLogin() {
        val user = firebaseAuth.currentUser ?: return

        firestore.collection("usuarios").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nomeUsuario = document.getString("nome") ?: "Usuário"
                    @Suppress("UNCHECKED_CAST")
                    val funcoes = document.get("funcoes") as? HashMap<String, String>

                    if (funcoes.isNullOrEmpty()) {
                        Toast.makeText(this, "Este usuário não possui papéis definidos.", Toast.LENGTH_LONG).show()
                        firebaseAuth.signOut()
                        return@addOnSuccessListener
                    }

                    if (funcoes.size > 1) {
                        val bottomSheet = SelecionarPerfilSheet.newInstance(funcoes, nomeUsuario)
                        bottomSheet.show(supportFragmentManager, "SelecionarPerfilSheet")
                    } else {
                        val (rede, papel) = funcoes.entries.first()
                        salvarRedeSelecionada(rede) // Salva a rede selecionada
                        navegarParaTelaCorreta(rede, papel)
                    }
                } else {
                    Toast.makeText(this, "Dados do usuário não encontrados.", Toast.LENGTH_LONG).show()
                    firebaseAuth.signOut()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Erro ao buscar dados: ${exception.message}", Toast.LENGTH_SHORT).show()
                Log.e("LOGIN_ERROR", "Erro ao buscar documento do usuário", exception)
            }
    }

    override fun onPerfilSelecionado(rede: String, papel: String) {
        Toast.makeText(this, "Entrando como $papel da $rede", Toast.LENGTH_SHORT).show()
        salvarRedeSelecionada(rede) // Salva a rede selecionada aqui também!
        navegarParaTelaCorreta(rede, papel)
    }

    override fun onLogoutSelecionado() {
        // Na tela de login, o logout não faz nada
    }

    /**
     * Salva a rede selecionada nas SharedPreferences.
     * @param rede A string da rede a ser salva.
     */
    private fun salvarRedeSelecionada(rede: String) {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putString("REDE_SELECIONADA", rede)
            apply()
        }
    }

    /**
     * Navega para a tela correta com base no papel do usuário e limpa a pilha de Activities.
     * @param rede A rede selecionada para o perfil.
     * @param papel O papel do usuário (pastor, lider, secretario).
     */
    private fun navegarParaTelaCorreta(rede: String, papel: String?) {
        Log.d("FLUXO_APP", "MainActivity: Navegando para papel: $papel, rede: $rede")
        val intent = when (papel) {
            "pastor" -> Intent(this, PastorDashboardActivity::class.java)
            "lider" -> Intent(this, LiderDashboardActivity::class.java) // Nome da sua Activity de gráficos
            "secretario" -> Intent(this, SecretarioDashboardActivity::class.java)
            else -> null
        }

        if (intent != null) {
            intent.putExtra("REDE_SELECIONADA", rede) // Ainda útil para o 1º carregamento ou debug

            // Estas flags são CRUCIAIS para garantir que as Activities anteriores sejam recriadas
            // quando a rede mudar. Elas limpam a pilha de tarefas atual e iniciam a nova Activity
            // como a raiz de uma nova tarefa.
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish() // Finaliza a MainActivity para que o usuário não volte para o login pelo botão voltar
        } else {
            Toast.makeText(this, "Papel '$papel' desconhecido.", Toast.LENGTH_LONG).show()
            firebaseAuth.signOut()
        }
    }
}