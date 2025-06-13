package com.ibf.app

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LiderDashboardActivity : AppCompatActivity(), SelecionarPerfilSheet.PerfilSelecionadoListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var nomeUsuario: String? = null

    // CORREÇÃO 1: Unificamos as variáveis. Usaremos apenas 'redeSelecionada' que já recebe o valor do Intent.
    private var redeSelecionada: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lider_dashboard)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        redeSelecionada = intent.getStringExtra("REDE_SELECIONADA")
        if (redeSelecionada == null) {
            Toast.makeText(this, "Erro: Rede não encontrada. Fazendo logout.", Toast.LENGTH_LONG).show()
            fazerLogout()
            return
        }

        val greetingText = findViewById<TextView>(R.id.text_greeting)

        buscarNomeDoUsuario { nome ->
            nomeUsuario = nome
            greetingText.text = getString(R.string.saudacao_lider, nome.split(" ").firstOrNull() ?: "")
        }

        setupMenuActions()
        setupNavigation()
    }

    override fun onPerfilSelecionado(rede: String, papel: String) {
        if (papel != "lider") {
            navegarParaTelaCorreta(rede, papel)
        } else {
            this.redeSelecionada = rede
            Toast.makeText(this, "Visão alterada para a $rede", Toast.LENGTH_SHORT).show()
            // Recarregar os dados da UI que dependem da rede, se houver
        }
    }

    override fun onLogoutSelecionado() {
        fazerLogout()
    }

    private fun setupMenuActions() {
        val cardRelatorios = findViewById<MaterialCardView>(R.id.card_relatorios)
        val cardGraficos = findViewById<MaterialCardView>(R.id.card_graficos)
        val cardMembros = findViewById<MaterialCardView>(R.id.card_membros)
        val cardConfig = findViewById<MaterialCardView>(R.id.card_config)
        val cardMudarPerfil = findViewById<MaterialCardView>(R.id.card_mudar_perfil)

        // CORREÇÃO 2: Todos os cards agora chamam a função 'abrirTela' para um código mais limpo e consistente.
        cardRelatorios.setOnClickListener {
            abrirTela(LiderStatusRelatoriosActivity::class.java)
        }

        cardGraficos.setOnClickListener {
            abrirTela(LiderGraficosActivity::class.java)
        }

        cardMembros.setOnClickListener { Toast.makeText(this, "Lista de Membros em breve!", Toast.LENGTH_SHORT).show() }
        cardConfig.setOnClickListener { Toast.makeText(this, "Configurações em breve!", Toast.LENGTH_SHORT).show() }

        cardMudarPerfil.setOnClickListener {
            abrirSeletorDePerfil()
        }
    }

    // CORREÇÃO 3: A função 'abrirTela' agora é inteligente e reutilizável.
    // Ela recebe como parâmetro a tela que deve abrir.
    private fun <T> abrirTela(activityClass: Class<T>) {
        if (!redeSelecionada.isNullOrEmpty()) {
            val intent = Intent(this, activityClass)
            intent.putExtra("REDE_SELECIONADA", redeSelecionada)
            startActivity(intent)
        } else {
            Toast.makeText(this, "Aguarde, carregando dados da rede...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.navigation_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> true
                R.id.navigation_reports -> {
                    abrirTela(LiderStatusRelatoriosActivity::class.java)
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

    private fun buscarNomeDoUsuario(callback: (String) -> Unit) {
        val user = auth.currentUser ?: return
        firestore.collection("usuarios").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nome = document.getString("nome") ?: "Líder"
                    callback(nome)
                } else { callback("Líder") }
            }.addOnFailureListener { callback("Líder") }
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

    private fun fazerLogout() {
        auth.signOut()
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
            if (this::class.java.simpleName == intent.component?.shortClassName?.removePrefix(".")) {
                this.redeSelecionada = rede
                Toast.makeText(this, "Visão alterada para a $rede", Toast.LENGTH_SHORT).show()
                return
            }
            intent.putExtra("REDE_SELECIONADA", rede)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}