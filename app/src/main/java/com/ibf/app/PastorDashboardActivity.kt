package com.ibf.app

import android.annotation.SuppressLint
import android.content.Context // Importação necessária para SharedPreferences
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView // Importação para TextView
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore // Adicionado para buscar dados do usuário

// Adicionamos a interface para "ouvir" a seleção de perfil
class PastorDashboardActivity : AppCompatActivity(), SelecionarPerfilSheet.PerfilSelecionadoListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore // Adicionado para buscar dados do usuário
    private var redeSelecionada: String? = null
    private lateinit var textRedeAtual: TextView // Novo TextView para mostrar a rede
    private lateinit var greetingText: TextView // Para o texto de boas-vindas do pastor

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pastor_dashboard)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Inicializa o TextView para a rede atual (você precisará adicionar este ID ao seu XML)
        textRedeAtual = findViewById(R.id.text_rede_dashboard) // **VERIFIQUE/ADICIONE ESTE ID NO SEU XML**
        greetingText = findViewById(R.id.text_greeting) // **VERIFIQUE/ADICIONE ESTE ID NO SEU XML**

        // --- Lógica de Carregamento da Rede na Criação ---
        val redeInPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("REDE_SELECIONADA", null)

        redeSelecionada = redeInPrefs ?: intent.getStringExtra("REDE_SELECIONADA")

        if (redeSelecionada == null) {
            Toast.makeText(this, "Erro: Rede não especificada. Fazendo logout.", Toast.LENGTH_LONG).show()
            fazerLogout()
            return
        }

        // Atualiza o TextView da rede no dashboard
        textRedeAtual.text = "Rede: ${redeSelecionada}"

        // Configura o texto de boas-vindas do pastor
        firestore.collection("usuarios").document(auth.currentUser!!.uid).get()
            .addOnSuccessListener { document ->
                val nome = document.getString("nome") ?: "Pastor"
                greetingText.text = "Olá, Pastor $nome"
            }

        // Configura a navegação inferior e o botão de perfil
        setupNavigationAndProfileButton()

        // Aqui você chamaria funções para carregar quaisquer dados específicos do Pastor
        // que dependam da 'redeSelecionada'. Ex:
        // carregarDadosEstatisticosPastor()
    }

    override fun onResume() {
        super.onResume()
        // --- Lógica para verificar se a rede mudou e recarregar dados ---
        val currentRedeInPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("REDE_SELECIONADA", null)

        if (currentRedeInPrefs != null && currentRedeInPrefs != redeSelecionada) {
            redeSelecionada = currentRedeInPrefs
            textRedeAtual.text = "Rede: ${redeSelecionada}" // Atualiza o texto da rede
            // Chame aqui qualquer função que carregue dados para o dashboard do Pastor
            // Ex: carregarDadosEstatisticosPastor()
            Toast.makeText(this, "Dados do Pastor atualizados para a rede: $redeSelecionada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupNavigationAndProfileButton() {
        val profileImage = findViewById<ImageView>(R.id.image_profile)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.navigation_home // Garante que 'home' comece selecionado

        // Define a ação de clique para a IMAGEM DE PERFIL
        profileImage.setOnClickListener {
            abrirSeletorDePerfil() // Agora abre o seletor de perfil, não apenas logout
        }

        // Define a lógica para a barra de navegação inferior
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    Toast.makeText(this, "Clicou em Início", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.navigation_reports -> {
                    Toast.makeText(this, "Clicou em Relatórios", Toast.LENGTH_SHORT).show()
                    // TODO: Pastor Dashboard pode ter uma tela de relatórios diferente
                    true
                }
                R.id.navigation_profile -> {
                    abrirSeletorDePerfil() // Abre seletor de perfil aqui também
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

    private fun fazerLogout() {
        auth.signOut()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // Função para navegar entre telas após seleção de perfil
    private fun navegarParaTelaCorreta(rede: String, papel: String?) {
        val intent = when (papel) {
            "pastor" -> Intent(this, PastorDashboardActivity::class.java)
            "lider" -> Intent(this, LiderDashboardActivity::class.java)
            "secretario" -> Intent(this, SecretarioDashboardActivity::class.java)
            else -> null
        }

        if (intent != null) {
            // Se o destino for a própria tela e a rede mudou
            if (this::class.java.simpleName == intent.component?.shortClassName?.removePrefix(".")) {
                if (rede != redeSelecionada) {
                    redeSelecionada = rede
                    val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    with (sharedPref.edit()) {
                        putString("REDE_SELECIONADA", rede)
                        apply()
                    }
                    textRedeAtual.text = "Rede: ${redeSelecionada}"
                    Toast.makeText(this, "Exibindo dados do Pastor para a rede: $rede", Toast.LENGTH_SHORT).show()
                    // Chame aqui a função para recarregar dados específicos do Pastor, se houver
                    // carregarDadosEstatisticosPastor()
                }
                return
            }

            intent.putExtra("REDE_SELECIONADA", rede)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    // --- MÉTODOS DA INTERFACE 'PerfilSelecionadoListener' ---
    override fun onPerfilSelecionado(rede: String, papel: String) {
        navegarParaTelaCorreta(rede, papel)
    }

    override fun onLogoutSelecionado() {
        fazerLogout()
    }
}