package com.ibf.app.ui.dashboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ibf.app.R
import com.ibf.app.ui.configuracoes.ConfiguracoesRedeActivity
import com.ibf.app.ui.graficos.LiderGraficosActivity
import com.ibf.app.ui.main.MainActivity
import com.ibf.app.ui.perfil.PerfilActivity
import com.ibf.app.ui.relatorios.LiderStatusRelatoriosActivity
import com.ibf.app.ui.shared.SelecionarPerfilSheet
import com.ibf.app.ui.usuarios.MembrosRedeActivity

class LiderDashboardActivity : AppCompatActivity(), SelecionarPerfilSheet.PerfilSelecionadoListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var redeSelecionada: String? = null
    private var papelUsuarioLogado: String? = null
    private lateinit var textRedeAtual: TextView
    private lateinit var textGreeting: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lider_dashboard)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        textRedeAtual = findViewById(R.id.text_rede_dashboard)
        textGreeting = findViewById(R.id.text_greeting)

        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        redeSelecionada = sharedPref.getString("REDE_SELECIONADA", null) ?: intent.getStringExtra("REDE_SELECIONADA")

        if (redeSelecionada == null) {
            Toast.makeText(this, getString(R.string.erro_rede_nao_especificada_logout), Toast.LENGTH_LONG).show()
            fazerLogout()
            return
        }

        textRedeAtual.text = getString(R.string.rede_dashboard_label, redeSelecionada)

        // Esta função agora busca os dados E configura os botões depois
        buscarDadosEConfigurarTela()
        setupNavigation()
    }

    private fun buscarDadosEConfigurarTela() {
        auth.currentUser?.uid?.let { uid ->
            firestore.collection("usuarios").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val nome = document.getString("nome") ?: getString(R.string.usuario_padrao)
                        textGreeting.text = getString(R.string.ola_nome, nome)

                        @Suppress("UNCHECKED_CAST")
                        val funcoes = document.get("funcoes") as? HashMap<String, String>
                        papelUsuarioLogado = funcoes?.get(redeSelecionada)
                        if (papelUsuarioLogado == null) {
                            papelUsuarioLogado = funcoes?.get("geral")
                        }

                        // --- CORREÇÃO APLICADA AQUI ---
                        // A configuração dos botões agora acontece aqui, DEPOIS que os dados chegaram.
                        configurarBotoesDoMenu()
                    } else {
                        Toast.makeText(this, "Utilizador não encontrado no banco de dados.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("LiderDashboard", "Erro ao buscar dados do utilizador: ${e.message}")
                    Toast.makeText(this, getString(R.string.erro_carregar_dados_usuario), Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun configurarBotoesDoMenu() {
        findViewById<MaterialCardView>(R.id.card_relatorios).setOnClickListener {
            val intent = Intent(this, LiderStatusRelatoriosActivity::class.java)
            intent.putExtra("REDE_SELECIONADA", redeSelecionada)
            startActivity(intent)
        }

        findViewById<MaterialCardView>(R.id.card_graficos).setOnClickListener {
            val intent = Intent(this, LiderGraficosActivity::class.java)
            intent.putExtra("REDE_SELECIONADA", redeSelecionada)
            startActivity(intent)
        }

        findViewById<MaterialCardView>(R.id.card_membros).setOnClickListener {
            val intent = Intent(this, MembrosRedeActivity::class.java)
            intent.putExtra("REDE_SELECIONADA", redeSelecionada)
            intent.putExtra("PAPEL_USUARIO_LOGADO", papelUsuarioLogado)
            startActivity(intent)
        }

        findViewById<MaterialCardView>(R.id.card_config).setOnClickListener {
            if (papelUsuarioLogado != null) {
                val intent = Intent(this, ConfiguracoesRedeActivity::class.java)
                intent.putExtra("REDE_SELECIONADA", redeSelecionada)
                intent.putExtra("PAPEL_USUARIO_LOGADO", papelUsuarioLogado)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Aguarde, a carregar dados do perfil...", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<MaterialCardView>(R.id.card_mudar_perfil).setOnClickListener {
            abrirSeletorDePerfil()
        }
    }

    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.navigation_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> true
                R.id.navigation_profile -> {
                    val intent = Intent(this, PerfilActivity::class.java)
                    intent.putExtra("REDE_SELECIONADA", redeSelecionada)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    private fun abrirSeletorDePerfil() {
        val user = auth.currentUser ?: return
        firestore.collection("usuarios").document(user.uid).get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val nomeUsuario = document.getString("nome") ?: getString(R.string.usuario_padrao)
                @Suppress("UNCHECKED_CAST")
                val funcoes = document.get("funcoes") as? HashMap<String, String>
                if (!funcoes.isNullOrEmpty()) {
                    val bottomSheet = SelecionarPerfilSheet.newInstance(funcoes, nomeUsuario)
                    bottomSheet.show(supportFragmentManager, "SelecionarPerfilSheet")
                }
            }
        }
    }

    @SuppressLint("StringFormatMatches")
    override fun onPerfilSelecionado(rede: String, papel: String) {
        navegarParaTelaCorreta(rede, papel)
    }

    override fun onLogoutSelecionado() {
        fazerLogout()
    }

    private fun fazerLogout() {
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
            val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            sharedPref.edit {
                putString("REDE_SELECIONADA", rede)
                putString("PAPEL_SELECIONADO", papel)
            }
            intent.putExtra("REDE_SELECIONADA", rede)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, getString(R.string.papel_desconhecido, papel), Toast.LENGTH_LONG).show()
            fazerLogout()
        }
    }
}