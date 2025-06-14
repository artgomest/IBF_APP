package com.ibf.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Adicionamos a interface para "ouvir" a seleção de perfil, igual ao secretário
class LiderDashboardActivity : AppCompatActivity(), SelecionarPerfilSheet.PerfilSelecionadoListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var redeSelecionada: String? = null // Rede que a Activity está exibindo
    private var papelUsuarioLogado: String? = "lider" // <--- Assumindo 'lider' como padrão para este dashboard
    private lateinit var textRedeAtual: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lider_dashboard)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        textRedeAtual = findViewById(R.id.text_rede_dashboard)

        // --- Lógica de Carregamento da Rede na Criação (mantida do código anterior) ---
        val redeInPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("REDE_SELECIONADA", null)

        redeSelecionada = redeInPrefs ?: intent.getStringExtra("REDE_SELECIONADA")

        if (redeSelecionada == null) {
            Toast.makeText(this, "Erro: Rede não especificada. Fazendo logout.", Toast.LENGTH_LONG).show()
            fazerLogout()
            return
        }

        textRedeAtual.text = "Rede: ${redeSelecionada}"

        val greetingText = findViewById<TextView>(R.id.text_greeting)
        firestore.collection("usuarios").document(auth.currentUser!!.uid).get()
            .addOnSuccessListener { document ->
                val nome = document.getString("nome") ?: "Líder"
                greetingText.text = "Olá, $nome"

                // AQUI: Buscar o papel real do usuário logado para a rede selecionada
                @Suppress("UNCHECKED_CAST")
                val funcoes = document.get("funcoes") as? HashMap<String, String>
                // Encontra o papel "lider" para a rede selecionada
                papelUsuarioLogado = funcoes?.get(redeSelecionada) // Obtém o papel específico para a rede
                if (papelUsuarioLogado == null) {
                    // Caso o papel não seja encontrado para a rede, trate o erro
                    Log.e("LiderDashboard", "Papel do usuário não encontrado para a rede $redeSelecionada")
                    // Opcional: Toast.makeText(this, "Erro: Seu papel para esta rede não foi definido.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("LiderDashboard", "Erro ao buscar dados do usuário: ${e.message}")
                Toast.makeText(this, "Erro ao carregar dados do usuário.", Toast.LENGTH_LONG).show()
            }

        setupNavigation()

        // Card de Relatórios
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_relatorios).setOnClickListener {
            val intent = Intent(this, LiderStatusRelatoriosActivity::class.java)
            intent.putExtra("REDE_SELECIONADA", redeSelecionada)
            startActivity(intent)
        }

        // Card de Gráficos
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_graficos).setOnClickListener {
            val intent = Intent(this, LiderGraficosActivity::class.java)
            intent.putExtra("REDE_SELECIONADA", redeSelecionada)
            startActivity(intent)
        }

        // Card de Membros
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_membros).setOnClickListener {
            Toast.makeText(this, "Função de Membros a ser implementada", Toast.LENGTH_SHORT).show()
        }

        // Card de Configurações
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_config).setOnClickListener {
            // Verifica se a rede e o papel estão disponíveis antes de iniciar a Activity
            if (redeSelecionada != null && papelUsuarioLogado != null) { // <--- VERIFICAÇÃO AQUI
                val intent = Intent(this, ConfiguracoesRedeActivity::class.java)
                intent.putExtra("REDE_SELECIONADA", redeSelecionada) // <--- PASSANDO A REDE
                intent.putExtra("PAPEL_USUARIO_LOGADO", papelUsuarioLogado) // <--- PASSANDO O PAPEL
                startActivity(intent)
            } else {
                Toast.makeText(this, "Informações da rede ou papel ausentes. Tente novamente.", Toast.LENGTH_SHORT).show()
                Log.e("LiderDashboard", "Tentativa de abrir Configurações com rede=$redeSelecionada ou papel=$papelUsuarioLogado nulo.")
            }
        }

        val cardMudarPerfil = findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_mudar_perfil)
        cardMudarPerfil.setOnClickListener {
            abrirSeletorDePerfil()
        }
    }

    override fun onResume() {
        super.onResume()
        // --- Lógica para verificar se a rede mudou e recarregar dados ---
        val currentRedeInPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("REDE_SELECIONADA", null)

        if (currentRedeInPrefs != null && currentRedeInPrefs != redeSelecionada) {
            redeSelecionada = currentRedeInPrefs
            textRedeAtual.text = "Rede: ${redeSelecionada}"
            // Re-busca o papel para a nova rede selecionada, se necessário
            auth.currentUser?.uid?.let { uid ->
                firestore.collection("usuarios").document(uid).get()
                    .addOnSuccessListener { document ->
                        @Suppress("UNCHECKED_CAST")
                        val funcoes = document.get("funcoes") as? HashMap<String, String>
                        papelUsuarioLogado = funcoes?.get(redeSelecionada)
                        Log.d("LiderDashboard", "Papel atualizado para $papelUsuarioLogado na rede $redeSelecionada")
                    }
            }
            Toast.makeText(this, "Dados do dashboard atualizados para a rede: $redeSelecionada", Toast.LENGTH_SHORT).show()
            // Se houver qualquer dado visual no dashboard que dependa da rede (além do texto),
            // chame uma função para recarregar esses dados aqui.
            // Por exemplo: carregarMetricasDoDashboardParaRede(redeSelecionada!!)
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
                val nomeUsuario = document.getString("nome") ?: "Usuário"
                @Suppress("UNCHECKED_CAST")
                val funcoes = document.get("funcoes") as? HashMap<String, String>
                if (!funcoes.isNullOrEmpty()) {
                    val bottomSheet = SelecionarPerfilSheet.newInstance(funcoes, nomeUsuario)
                    bottomSheet.show(supportFragmentManager, "SelecionarPerfilSheet")
                }
            }
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
            if (this::class.java.simpleName == intent.component?.shortClassName?.removePrefix(".")) {
                if (rede != redeSelecionada) {
                    redeSelecionada = rede
                    // Atualiza o papelUsuarioLogado também se a rede mudou
                    auth.currentUser?.uid?.let { uid ->
                        firestore.collection("usuarios").document(uid).get()
                            .addOnSuccessListener { document ->
                                @Suppress("UNCHECKED_CAST")
                                val funcoes = document.get("funcoes") as? HashMap<String, String>
                                papelUsuarioLogado = funcoes?.get(redeSelecionada)
                            }
                    }
                    val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    with (sharedPref.edit()) {
                        putString("REDE_SELECIONADA", rede)
                        apply()
                    }
                    textRedeAtual.text = "Rede: ${redeSelecionada}"
                    Toast.makeText(this, "Exibindo dados da $rede", Toast.LENGTH_SHORT).show()
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
        navegarParaTelaCorreta(rede, papel)
    }

    override fun onLogoutSelecionado() {
        fazerLogout()
    }
}