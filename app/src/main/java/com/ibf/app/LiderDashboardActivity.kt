package com.ibf.app

import android.content.Intent
import android.os.Bundle
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
    private var redeSelecionada: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lider_dashboard)

        redeSelecionada = intent.getStringExtra("REDE_SELECIONADA")
        if (redeSelecionada == null) {
            Toast.makeText(this, "Erro: Rede não especificada.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Configura o texto de boas-vindas
        val greetingText = findViewById<TextView>(R.id.text_greeting)
        firestore.collection("usuarios").document(auth.currentUser!!.uid).get()
            .addOnSuccessListener { document ->
                val nome = document.getString("nome") ?: "Líder"
                greetingText.text = "Olá, $nome"
            }

        // Configura a navegação, igual ao secretário
        setupNavigation()

        // Lembre-se de adicionar os OnClickListeners para seus cards aqui...
        // Ex: findViewById<MaterialCardView>(R.id.card_graficos).setOnClickListener { ... }
    }

    // Função replicada do Secretário para configurar o menu inferior
    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.navigation_home // Garante que 'home' comece selecionado

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> true // Já estamos em casa, não faz nada

                R.id.navigation_reports -> {
                    // AÇÃO IDÊNTICA À DO SECRETÁRIO: Abre a bandeja para selecionar o tipo de relatório
                    val bottomSheet = SelecionarRelatorioSheet()
                    bottomSheet.show(supportFragmentManager, "SelecionarRelatorioSheet")
                    true
                }

                R.id.navigation_profile -> {
                    // AÇÃO IDÊNTICA À DO SECRETÁRIO: Abre a bandeja para trocar de perfil ou sair
                    abrirSeletorDePerfil()
                    true
                }
                else -> false
            }
        }
    }

    // Função replicada do Secretário para abrir a bandeja de perfis
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

    // Função replicada do Secretário para navegar entre telas
    private fun navegarParaTelaCorreta(rede: String, papel: String?) {
        val intent = when (papel) {
            "pastor" -> Intent(this, PastorDashboardActivity::class.java)
            "lider" -> Intent(this, LiderDashboardActivity::class.java)
            "secretario" -> Intent(this, SecretarioDashboardActivity::class.java)
            else -> null
        }

        if (intent != null) {
            // Se o destino for a própria tela, não faz nada
            if (this::class.java.simpleName == intent.component?.shortClassName?.removePrefix(".")) {
                Toast.makeText(this, "Já está na tela do $papel da $rede", Toast.LENGTH_SHORT).show()
                return
            }
            intent.putExtra("REDE_SELECIONADA", rede)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    // Função replicada do Secretário para fazer logout
    private fun fazerLogout() {
        auth.signOut()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // --- MÉTODOS DA INTERFACE 'PerfilSelecionadoListener' ---

    override fun onPerfilSelecionado(rede: String, papel: String) {
        // Se selecionar um perfil diferente, navega para a tela correta
        navegarParaTelaCorreta(rede, papel)
    }

    override fun onLogoutSelecionado() {
        fazerLogout()
    }
}