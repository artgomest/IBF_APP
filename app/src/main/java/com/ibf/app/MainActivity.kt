package com.ibf.app

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
                        navegarParaTelaCorreta(rede, papel) // Agora passamos a rede também
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
        navegarParaTelaCorreta(rede, papel) // Passamos a rede escolhida
    }

    override fun onLogoutSelecionado() {
        // Na tela de login, o logout não faz nada
    }

    // A função de navegação agora também recebe a REDE
    private fun navegarParaTelaCorreta(rede: String, papel: String?) {
        val intent = when (papel) {
            "pastor" -> Intent(this, PastorDashboardActivity::class.java)
            "lider" -> Intent(this, LiderDashboardActivity::class.java)
            "secretario" -> Intent(this, SecretarioDashboardActivity::class.java)
            else -> null
        }

        if (intent != null) {
            // Adicionamos a rede selecionada como um "extra" no intent
            intent.putExtra("REDE_SELECIONADA", rede)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Papel '$papel' desconhecido.", Toast.LENGTH_LONG).show()
            firebaseAuth.signOut()
        }
    }
}