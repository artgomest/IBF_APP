package com.ibf.app.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.ibf.app.R
import com.ibf.app.ui.dashboard.LiderDashboardActivity
import com.ibf.app.ui.dashboard.PastorDashboardActivity
import com.ibf.app.ui.dashboard.SecretarioDashboardActivity
import com.ibf.app.ui.shared.SelecionarPerfilSheet
import com.ibf.app.ui.usuarios.SolicitacaoCadastroActivity

class MainActivity : AppCompatActivity(), SelecionarPerfilSheet.PerfilSelecionadoListener {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var textCadastreSe: TextView

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Permissão para notificações concedida!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissão para notificações negada.", Toast.LENGTH_SHORT).show()
            }
        }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
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
                        // --- CORREÇÃO APLICADA AQUI ---
                        // Garante que o token seja salvo no perfil do usuário que acabou de logar
                        task.result?.user?.uid?.let { uid ->
                            salvarTokenDoDispositivo(uid)
                        }
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
        pedirPermissaoDeNotificacao()
    }

    // --- FUNÇÃO ADICIONADA ---
    private fun salvarTokenDoDispositivo(uid: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("MainActivity", "Falha ao obter token do FCM.", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            val userDocument = firestore.collection("usuarios").document(uid)

            // Usamos .set com merge=true para criar o campo se ele não existir,
            // ou apenas atualizá-lo se já existir, sem apagar outros dados.
            userDocument.set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener { Log.d("MainActivity", "Token FCM salvo para o usuário: $uid") }
                .addOnFailureListener { e -> Log.e("MainActivity", "Erro ao salvar token FCM", e) }
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
        salvarRedeSelecionada(rede)
        navegarParaTelaCorreta(rede, papel)
    }

    override fun onLogoutSelecionado() {
        fazerLogout()
    }

    private fun fazerLogout() {
        auth.signOut()
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        sharedPref.edit { clear() }
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun salvarRedeSelecionada(rede: String) {
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
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

    // Anotação adicionada para remover o aviso de API
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun pedirPermissaoDeNotificacao() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}