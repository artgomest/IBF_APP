package com.ibf.app.ui.dashboard

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

        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val redeInPrefs = sharedPref.getString("REDE_SELECIONADA", null)

        redeSelecionada = redeInPrefs ?: intent.getStringExtra("REDE_SELECIONADA")

        if (redeSelecionada == null) {
            Toast.makeText(this, getString(R.string.erro_rede_nao_especificada_logout), Toast.LENGTH_LONG).show()
            fazerLogout()
            return
        }

        textRedeAtual.text = getString(R.string.rede_dashboard_label, redeSelecionada)

        auth.currentUser?.uid?.let { uid ->
            firestore.collection("usuarios").document(uid).get()
                .addOnSuccessListener { document ->
                    val nome = document.getString("nome") ?: getString(R.string.usuario_padrao)
                    textGreeting.text = getString(R.string.ola_nome, nome)

                    @Suppress("UNCHECKED_CAST")
                    val funcoes = document.get("funcoes") as? HashMap<String, String>
                    papelUsuarioLogado = funcoes?.get(redeSelecionada)
                    if (papelUsuarioLogado == null) {
                        papelUsuarioLogado = funcoes?.get("geral")
                    }

                    if (papelUsuarioLogado == null) {
                        Log.e("LiderDashboard", "Papel do usuário não encontrado para a rede $redeSelecionada")
                        Toast.makeText(this, getString(R.string.erro_papel_nao_definido), Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("LiderDashboard", "Erro ao buscar dados do usuário: ${e.message}")
                    Toast.makeText(this, getString(R.string.erro_carregar_dados_usuario), Toast.LENGTH_LONG).show()
                }
        }

        setupNavigation()

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
            Toast.makeText(this, getString(R.string.funcao_membros_implementar), Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialCardView>(R.id.card_config).setOnClickListener {
            if (redeSelecionada != null && papelUsuarioLogado != null) {
                val intent = Intent(this, ConfiguracoesRedeActivity::class.java)
                intent.putExtra("REDE_SELECIONADA", redeSelecionada)
                intent.putExtra("PAPEL_USUARIO_LOGADO", papelUsuarioLogado)
                startActivity(intent)
            } else {
                Toast.makeText(this, getString(R.string.info_rede_papel_ausentes), Toast.LENGTH_SHORT).show()
                Log.e("LiderDashboard", "Tentativa de abrir Configurações com rede=$redeSelecionada ou papel=$papelUsuarioLogado nulo.")
            }
        }

        val cardMudarPerfil = findViewById<MaterialCardView>(R.id.card_mudar_perfil)
        cardMudarPerfil.setOnClickListener {
            abrirSeletorDePerfil()
        }
    }

    override fun onResume() {
        super.onResume()
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val currentRedeInPrefs = sharedPref.getString("REDE_SELECIONADA", null)

        // IMPORTANTE: VERIFICA SE A REDE MUDOU NAS PREFERÊNCIAS
        if (currentRedeInPrefs != null && currentRedeInPrefs != redeSelecionada) {
            redeSelecionada = currentRedeInPrefs // Atualiza a rede da Activity
            textRedeAtual.text = getString(R.string.rede_dashboard_label, redeSelecionada) // Atualiza UI

            // Recarrega o papel do usuário para a nova rede
            auth.currentUser?.uid?.let { uid ->
                firestore.collection("usuarios").document(uid).get()
                    .addOnSuccessListener { document ->
                        @Suppress("UNCHECKED_CAST")
                        val funcoes = document.get("funcoes") as? HashMap<String, String>
                        papelUsuarioLogado = funcoes?.get(redeSelecionada)
                        if (papelUsuarioLogado == null) {
                            papelUsuarioLogado = funcoes?.get("geral")
                        }
                        Log.d("LiderDashboard", "Papel atualizado para $papelUsuarioLogado na rede $redeSelecionada")
                    }
            }
            // Se houver dados visuais no dashboard que dependam da rede, chame uma função para recarregá-los.
            // Por exemplo: carregarMetricasDoDashboard(redeSelecionada!!)
            Toast.makeText(this, getString(R.string.dados_atualizados_rede, redeSelecionada), Toast.LENGTH_SHORT).show()
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

    // --- MÉTODOS DA INTERFACE 'PerfilSelecionadoListener' ---
    override fun onPerfilSelecionado(rede: String, papel: String) {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        // --- CORREÇÃO CRÍTICA: SALVAR A NOVA REDE NAS SHARED PREFERENCES ---
        sharedPref.edit {
            putString("REDE_SELECIONADA", rede)
        }

        // Se a nova rede for a mesma que já está exibindo (e o papel é o mesmo do dashboard),
        // apenas avisa e recarrega. Isso evita recriar a Activity desnecessariamente.
        if (rede == redeSelecionada && papel == papelUsuarioLogado?.let { getRoleFromMap(it) }) { // Compara apenas o papel principal do dashboard
            Toast.makeText(this, getString(R.string.ja_exibindo_dados_rede, rede, papel), Toast.LENGTH_SHORT).show()
            // Como a rede nas preferências foi atualizada, o onResume vai pegar a mudança
            // e recarregar os dados, mesmo que a Activity não tenha sido recriada.
            redeSelecionada = rede // Atualiza localmente para onResume não re-detectar
            // Não chame carregarDados aqui diretamente, deixe o onResume fazer o trabalho
            return
        }

        // Se a rede ou papel principal (lider/pastor/secretario) mudou, navega para a tela correta,
        // limpando a pilha de volta para garantir que a Activity seja recriada (ou receba onResume).
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
            fazerLogout()
        }
    }

    override fun onLogoutSelecionado() {
        fazerLogout()
    }

    private fun fazerLogout() {
        auth.signOut()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // Helper para extrair o papel "principal" se funcoes for um mapa mais complexo
    private fun getRoleFromMap(papel: String): String {
        // Se seu papelUsuarioLogado já é "lider" ou "pastor", apenas retorne.
        // Se for algo como "Rede Alpha:lider", você precisaria parsear.
        return papel.substringBefore(":") // Ex: "lider" de "Rede Alpha:lider"
    }
}