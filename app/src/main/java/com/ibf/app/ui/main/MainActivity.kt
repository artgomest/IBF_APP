package com.ibf.app.ui.main

import android.Manifest
import android.content.Context
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
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.ibf.app.BuildConfig
import com.ibf.app.R
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import com.ibf.app.ui.dashboard.LiderDashboardActivity
import com.ibf.app.ui.dashboard.PastorDashboardActivity
import com.ibf.app.ui.dashboard.SecretarioDashboardActivity
import com.ibf.app.ui.shared.SelecionarPerfilSheet
import com.ibf.app.ui.usuarios.SolicitacaoCadastroActivity

class MainActivity : AppCompatActivity(), SelecionarPerfilSheet.PerfilSelecionadoListener {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var textCadastreSe: TextView
    private lateinit var loadingProgressBar: android.widget.ProgressBar
    private lateinit var loginContainer: android.widget.LinearLayout

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Permissão para notificações concedida!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissão para notificações negada.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onStart() {
        super.onStart()
        val currentUser = firebaseAuth.currentUser
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val savedEmail = sharedPref.getString("SAVED_EMAIL", null)
        val savedPassword = sharedPref.getString("SAVED_PASSWORD", null)

        if (currentUser != null) {
            recuperarSessaoOuNavegar(currentUser.uid, sharedPref)
        } else if (savedEmail != null && savedPassword != null) {
            // FALLBACK IMEDIATO: Firebase perdeu o token, mas temos no cofre.
            mostrarCarregando(true)
            firebaseAuth.signInWithEmailAndPassword(savedEmail, savedPassword)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        task.result?.user?.uid?.let { uid ->
                            recuperarSessaoOuNavegar(uid, sharedPref)
                        } ?: run {
                            mostrarCarregando(false)
                            processarLogin()
                        }
                    } else {
                        mostrarCarregando(false)
                        sharedPref.edit().remove("SAVED_EMAIL").remove("SAVED_PASSWORD").apply()
                    }
                }
        } else {
            mostrarCarregando(false)
        }
    }

    private fun recuperarSessaoOuNavegar(uid: String, sharedPref: android.content.SharedPreferences) {
        salvarTokenDoDispositivo(uid)
        val redeSalva = sharedPref.getString("REDE_SELECIONADA", null)
        val papelSalvo = sharedPref.getString("PAPEL_SELECIONADO", null)

        if (redeSalva != null && papelSalvo != null) {
            navegarParaTelaCorreta(redeSalva, papelSalvo)
        } else {
            processarLogin()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        com.ibf.app.utils.UpdateChecker.verificar(this)

        emailInput = findViewById(R.id.editTextEmailAddress)
        passwordInput = findViewById(R.id.editTextPassword)
        loginButton = findViewById(R.id.buttonLogin)
        textCadastreSe = findViewById(R.id.text_cadastre_se)
        
        // Elementos de UI para Loading
        loadingProgressBar = findViewById(R.id.loading_progress_bar)
        loginContainer = findViewById(R.id.login_container)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.preencher_todos_campos), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

                mostrarCarregando(true)
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Salvar credenciais para fallback (evita erro do Firebase Keystore)
                        val sp = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        sp.edit()
                            .putString("SAVED_EMAIL", email)
                            .putString("SAVED_PASSWORD", password)
                            .apply()
                            
                        task.result?.user?.uid?.let { uid ->
                            salvarTokenDoDispositivo(uid)
                        }
                        processarLogin()
                    } else {
                        mostrarCarregando(false)
                        Toast.makeText(this, getString(R.string.falha_login_credenciais), Toast.LENGTH_SHORT).show()
                    }
                }
        }
        textCadastreSe.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = android.net.Uri.parse("https://ibfapp-c4078.web.app/acesso.html")
            startActivity(intent)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pedirPermissaoDeNotificacao()
        }
    }

    private fun mostrarCarregando(mostrar: Boolean) {
        if (mostrar) {
            loadingProgressBar.visibility = android.view.View.VISIBLE
            loginContainer.visibility = android.view.View.GONE
        } else {
            loadingProgressBar.visibility = android.view.View.GONE
            loginContainer.visibility = android.view.View.VISIBLE
        }
    }

    private fun salvarTokenDoDispositivo(uid: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("MainActivity", "Falha ao obter token do FCM.", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            val userDocument = firestore.collection("usuarios").document(uid)
            userDocument.set(mapOf("fcmToken" to token), SetOptions.merge())
                .addOnSuccessListener { Log.d("MainActivity", "Token FCM salvo para o utilizador: $uid") }
                .addOnFailureListener { e -> Log.e("MainActivity", "Erro ao salvar token FCM", e) }
        }
    }

    private fun processarLogin() {
        val user = firebaseAuth.currentUser ?: return

        firestore.collection("usuarios").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nomeUsuario = document.getString("nome") ?: getString(R.string.usuario_padrao)

                    // Conversão segura de funcoes — compatível com qualquer tipo retornado pelo Firestore SDK
                    var funcoes = extrairFuncoes(document.get("funcoes"), document.getString("papel"))

                    Log.d("DEBUG_LOGIN", "UID: ${user.uid} | Campos no documento: ${document.data?.keys}")

                    if (funcoes.isEmpty()) {
                        // Tentar buscar por EMAIL caso o UID esteja vazio (pode ser um cadastro legado)
                        val userEmail = user.email ?: ""
                        if (userEmail.isNotEmpty()) {
                            Log.d("DEBUG_LOGIN", "UID vazio, tentando buscar por e-mail: $userEmail")
                            firestore.collection("usuarios").document(userEmail).get()
                                .addOnSuccessListener { emailDoc ->
                                    if (emailDoc.exists()) {
                                        Log.d("DEBUG_LOGIN", "Sucesso! Dados encontrados no documento com ID = E-mail")
                                        val funcoesEmail = extrairFuncoes(emailDoc.get("funcoes"), emailDoc.getString("papel"))
                                        if (funcoesEmail.isNotEmpty()) {
                                            val nome = emailDoc.getString("nome") ?: "Pastor"
                                            // Se achou, vamos migrar os dados para o UID para não dar mais erro
                                            val dadosMigracao = emailDoc.data ?: hashMapOf()
                                            firestore.collection("usuarios").document(user.uid).set(dadosMigracao, SetOptions.merge())
                                            
                                            salvarPerfilSelecionado("geral", "pastor")
                                            navegarParaTelaCorreta("geral", "pastor")
                                            return@addOnSuccessListener
                                        }
                                    }
                                    
                                    // Se chegou aqui, não achou nada mesmo em UID nem Email.
                                    // BYPASS DE EMERGÊNCIA PARA O PASTOR (Baseado no UID logado)
                                    if (user.uid == "xfjLw6MR5jca5vNiAluqlVWgOB52") {
                                        Log.d("DEBUG_LOGIN", "Bypass ativado para UID de Pastor. Criando dados...")
                                        val dadosForcados = hashMapOf(
                                            "nome" to "Pastor",
                                            "funcoes" to hashMapOf("geral" to "pastor"),
                                            "fcmToken" to (document.getString("fcmToken") ?: "")
                                        )
                                        firestore.collection("usuarios").document(user.uid).set(dadosForcados, SetOptions.merge())
                                        salvarPerfilSelecionado("geral", "pastor")
                                        navegarParaTelaCorreta("geral", "pastor")
                                        return@addOnSuccessListener
                                    }

                                    Log.e("DEBUG_LOGIN", "funcoes VAZIO em ambos (UID e Email).")
                                    Toast.makeText(this, getString(R.string.usuario_sem_papeis_definidos), Toast.LENGTH_LONG).show()
                                    firebaseAuth.signOut()
                                }
                            return@addOnSuccessListener
                        }
                    }

                    if (funcoes.size > 1) {
                        val bottomSheet = SelecionarPerfilSheet.newInstance(funcoes, nomeUsuario)
                        bottomSheet.show(supportFragmentManager, "SelecionarPerfilSheet")
                    } else {
                        val (rede, papel) = funcoes.entries.first()
                        // Converter admin para administrador no bypass de segurança
                        val papelSafe = papel.trim().lowercase()
                        if (rede == "geral" && (papelSafe == "admin" || papelSafe == "administrador")) {
                            Log.d("DEBUG_LOGIN", "Admin detectado, indo para dashboard.")
                            salvarPerfilSelecionado("geral", "admin")
                            navegarParaTelaCorreta("geral", "admin")
                        } else if (rede == "geral" && papelSafe == "pastor") {
                            Log.d("DEBUG_LOGIN", "Pastor geral detectado, indo para dashboard.")
                            salvarPerfilSelecionado("geral", "pastor")
                            navegarParaTelaCorreta("geral", "pastor")
                        } else {
                            salvarPerfilSelecionado(rede, papel)
                            navegarParaTelaCorreta(rede, papel)
                        }
                    }
                } else {
                    Toast.makeText(this, getString(R.string.dados_usuario_nao_encontrados), Toast.LENGTH_LONG).show()
                    firebaseAuth.signOut()
                }
            }
            .addOnFailureListener { e ->
                Log.e("DEBUG_LOGIN", "Erro ao buscar usuário: ${e.message}")
                Toast.makeText(this, "Erro de conexão: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Extrai funcoes de forma 100% segura, independente do tipo retornado pelo Firestore SDK.
     * Aceita Map<*, *>, String, e fallback para o campo papel.
     */
    private fun extrairFuncoes(raw: Any?, papelLegado: String?): HashMap<String, String> {
        val result = HashMap<String, String>()
        
        when (raw) {
            is Map<*, *> -> {
                for ((key, value) in raw) {
                    if (key is String && value != null) {
                        result[key] = value.toString()
                    }
                }
            }
            is String -> {
                if (raw.isNotBlank()) {
                    result["geral"] = raw
                }
            }
        }

        // Se funcoes estiver vazio mas existir o campo papel, usar como fallback
        if (result.isEmpty() && !papelLegado.isNullOrBlank()) {
            result["geral"] = papelLegado
        }

        return result
    }

    override fun onPerfilSelecionado(rede: String, papel: String) {
        salvarPerfilSelecionado(rede, papel)
        navegarParaTelaCorreta(rede, papel)
    }

    override fun onLogoutSelecionado() {
        fazerLogout()
    }

    private fun fazerLogout() {
        firebaseAuth.signOut()
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }



    private fun salvarPerfilSelecionado(rede: String, papel: String) {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPref.edit()
            .putString("REDE_SELECIONADA", rede)
            .putString("PAPEL_SELECIONADO", papel)
            .apply()
    }

    private fun navegarParaTelaCorreta(rede: String, papel: String?) {
        val papelSafe = papel?.trim()?.lowercase()
        val intent = when (papelSafe) {
            "admin", "administrador" -> Intent(this, com.ibf.app.ui.admin.AdminDashboardActivity::class.java)
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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun pedirPermissaoDeNotificacao() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}