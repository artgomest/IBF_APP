package com.ibf.app.ui.perfil

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ibf.app.R
import com.ibf.app.ui.dashboard.LiderDashboardActivity
import com.ibf.app.ui.dashboard.PastorDashboardActivity
import com.ibf.app.ui.dashboard.SecretarioDashboardActivity
import com.ibf.app.ui.main.MainActivity
import com.ibf.app.ui.shared.SelecionarPerfilSheet

class PerfilActivity : AppCompatActivity(), SelecionarPerfilSheet.PerfilSelecionadoListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var redeSelecionada: String? = null
    private lateinit var nomeEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var editarSalvarButton: Button

    private var modoEdicao = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        redeSelecionada = intent.getStringExtra("REDE_SELECIONADA")
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        nomeEditText = findViewById(R.id.edit_text_nome_perfil)
        emailEditText = findViewById(R.id.edit_text_email_perfil)
        editarSalvarButton = findViewById(R.id.button_editar_salvar)

        findViewById<ImageView>(R.id.button_back).setOnClickListener { finish() }

        carregarDadosUsuario()

        editarSalvarButton.setOnClickListener {
            if (modoEdicao) {
                salvarAlteracoes()
            } else {
                ativarModoEdicao()
            }
        }

        findViewById<Button>(R.id.button_estatisticas).setOnClickListener {
            val intent = Intent(this, EstatisticasActivity::class.java)
            intent.putExtra("REDE_SELECIONADA", redeSelecionada) // Passa a rede para a tela de estatísticas
            startActivity(intent)
        }

        findViewById<Button>(R.id.button_ajuda).setOnClickListener { 
            val url = "https://api.whatsapp.com/send?phone=5531987006343"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = android.net.Uri.parse(url)
            startActivity(intent)
        }

        findViewById<Button>(R.id.button_alternar_tema).setOnClickListener {
            val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val newMode = if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
            AppCompatDelegate.setDefaultNightMode(newMode)
        }
        
        findViewById<Button>(R.id.button_mudar_perfil).setOnClickListener {
            abrirSeletorDePerfil()
        }
    }

    private fun abrirSeletorDePerfil() {
        val user = auth.currentUser ?: return
        firestore.collection("usuarios").document(user.uid).get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val nomeUsuario = document.getString("nome") ?: getString(R.string.usuario_padrao)
                val funcoesRaw = document.get("funcoes") as? Map<*, *>
                val funcoes = HashMap<String, String>()
                funcoesRaw?.forEach { (k, v) -> if (k is String && v != null) funcoes[k] = v.toString() }
                if (funcoes.isNotEmpty()) {
                    val bottomSheet = SelecionarPerfilSheet.newInstance(funcoes, nomeUsuario)
                    bottomSheet.show(supportFragmentManager, "SelecionarPerfilSheet")
                } else {
                    Toast.makeText(this, "Nenhum perfil encontrado.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onPerfilSelecionado(rede: String, papel: String) {
        navegarParaTelaCorreta(rede, papel)
    }

    override fun onLogoutSelecionado() {
        auth.signOut()
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPref.edit { clear() }
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
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
            Toast.makeText(this, "Papel desconhecido: $papel", Toast.LENGTH_SHORT).show()
        }
    }

    private fun carregarDadosUsuario() {
        val user = auth.currentUser
        if (user != null) {
            firestore.collection("usuarios").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        nomeEditText.setText(document.getString("nome"))
                        emailEditText.setText(document.getString("email"))
                    }
                }
        }
    }

    private fun ativarModoEdicao() {
        modoEdicao = true
        nomeEditText.isEnabled = true
        nomeEditText.requestFocus()
        editarSalvarButton.text = getString(R.string.perfil_salvar)
    }

    private fun desativarModoEdicao() {
        modoEdicao = false
        nomeEditText.isEnabled = false
        editarSalvarButton.text = getString(R.string.perfil_editar)
    }

    private fun salvarAlteracoes() {
        val novoNome = nomeEditText.text.toString().trim()
        if (novoNome.isEmpty()) {
            nomeEditText.error = "O nome não pode ficar vazio"
            return
        }

        val user = auth.currentUser
        if (user != null) {
            editarSalvarButton.isEnabled = false
            firestore.collection("usuarios").document(user.uid)
                .update("nome", novoNome)
                .addOnSuccessListener {
                    Toast.makeText(this, getString(R.string.perfil_info_atualizadas), Toast.LENGTH_SHORT).show()
                    desativarModoEdicao()
                    editarSalvarButton.isEnabled = true
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "${getString(R.string.perfil_falha_atualizar)}: ${e.message}", Toast.LENGTH_LONG).show()
                    editarSalvarButton.isEnabled = true
                }
        }
    }
}