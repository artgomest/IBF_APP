package com.ibf.app

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

// Adicionamos a interface para "ouvir" a seleção de perfil, igual ao secretário
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
                        papelUsuarioLogado = funcoes?.get("geral") // Verifica se tem um papel 'geral'
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

        if (currentRedeInPrefs != null && currentRedeInPrefs != redeSelecionada) {
            redeSelecionada = currentRedeInPrefs
            textRedeAtual.text = getString(R.string.rede_dashboard_label, redeSelecionada)
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
            Toast.makeText(this, getString(R.string.dados_atualizados_rede, redeSelecionada), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.navigation_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> true

                R.id.navigation_reports -> {
                    val bottomSheet = SelecionarRelatorioSheet()
                    bottomSheet.show(supportFragmentManager, "SelecionarRelatorioSheet")
                    true
                }

                R.id.navigation_profile -> {
                    abrirSeletorDePerfil()
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

    // @Suppress("unused") // Pode ser adicionado para suprimir o warning, se desejar
    private fun navegarParaTelaCorreta(rede: String, papel: String?) {
        val intent = when (papel) {
            "pastor" -> Intent(this, PastorDashboardActivity::class.java)
            "lider" -> Intent(this, LiderDashboardActivity::class.java)
            "secretario" -> Intent(this, SecretarioDashboardActivity::class.java)
            else -> null
        }

        if (intent != null) {
            if (this::class.java.simpleName == intent.component?.shortClassName?.removePrefix(".")) {
                if (rede != redeSelecionada) {
                    val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    sharedPref.edit {
                        putString("REDE_SELECIONADA", rede)
                    }
                    this.redeSelecionada = rede
                    textRedeAtual.text = getString(R.string.rede_dashboard_label, redeSelecionada)
                    Toast.makeText(this, getString(R.string.exibindo_dados_rede, rede), Toast.LENGTH_SHORT).show()
                }
                return
            }

            intent.putExtra("REDE_SELECIONADA", rede)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun fazerLogout() {
        auth.signOut()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onPerfilSelecionado(rede: String, papel: String) {
        // A implementação deve ser a mesma da função navegarParaTelaCorreta
        // ou chamar diretamente navegarParaTelaCorreta
        navegarParaTelaCorreta(rede, papel)
    }

    override fun onLogoutSelecionado() {
        fazerLogout()
    }
}