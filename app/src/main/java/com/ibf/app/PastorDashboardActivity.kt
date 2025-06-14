package com.ibf.app // Seu package name correto

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Adicionamos a interface para "ouvir" a seleção de perfil
class PastorDashboardActivity : AppCompatActivity(), SelecionarPerfilSheet.PerfilSelecionadoListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var redeSelecionada: String? = null
    private var papelUsuarioLogado: String? = null // Adicionado para armazenar o papel do usuário logado
    private lateinit var textRedeAtual: TextView // Novo TextView para mostrar a rede
    private lateinit var greetingText: TextView // Para o texto de boas-vindas do pastor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pastor_dashboard)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Inicializa o TextView para a rede atual (você precisará adicionar este ID ao seu XML)
        textRedeAtual = findViewById(R.id.text_rede_dashboard)
        greetingText = findViewById(R.id.text_greeting)

        // --- Lógica de Carregamento da Rede na Criação ---
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val redeInPrefs = sharedPref.getString("REDE_SELECIONADA", null)

        redeSelecionada = redeInPrefs ?: intent.getStringExtra("REDE_SELECIONADA")

        if (redeSelecionada == null) {
            Toast.makeText(this, getString(R.string.erro_rede_nao_especificada_logout), Toast.LENGTH_LONG).show()
            fazerLogout()
            return
        }

        // --- CORREÇÃO DE WARNING (String literal) ---
        textRedeAtual.text = getString(R.string.rede_dashboard_label, redeSelecionada)

        // Configura o texto de boas-vindas do pastor e busca o papel
        auth.currentUser?.uid?.let { uid ->
            firestore.collection("usuarios").document(uid).get()
                .addOnSuccessListener { document ->
                    val nome = document.getString("nome") ?: getString(R.string.pastor_padrao)
                    // --- CORREÇÃO DE WARNING (String literal) ---
                    greetingText.text = getString(R.string.ola_pastor_nome, nome)

                    @Suppress("UNCHECKED_CAST")
                    val funcoes = document.get("funcoes") as? HashMap<String, String>
                    papelUsuarioLogado = funcoes?.get(redeSelecionada) // Pode ser 'pastor' ou outro se tiver papel específico para a rede
                    // Se o pastor tem um papel "geral", ele pode não ter um papel específico para cada rede.
                    // Adicione uma lógica para verificar o papel "geral" se aplicável.
                    if (papelUsuarioLogado == null) {
                        papelUsuarioLogado = funcoes?.get("geral") // Verifica se tem um papel 'geral'
                    }

                    if (papelUsuarioLogado == null) {
                        Log.e("PastorDashboard", "Papel do Pastor não encontrado para a rede $redeSelecionada ou geral.")
                        Toast.makeText(this, getString(R.string.erro_papel_nao_definido), Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("PastorDashboard", "Erro ao buscar dados do Pastor: ${e.message}")
                    Toast.makeText(this, getString(R.string.erro_carregar_dados_usuario), Toast.LENGTH_LONG).show()
                }
        }

        // Configura a navegação inferior e o botão de perfil
        setupNavigationAndProfileButton()

        // Aqui você chamaria funções para carregar quaisquer dados específicos do Pastor
        // que dependam da 'redeSelecionada'. Ex:
        // carregarDadosEstatisticosPastor()
    }

    override fun onResume() {
        super.onResume()
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val currentRedeInPrefs = sharedPref.getString("REDE_SELECIONADA", null)

        if (currentRedeInPrefs != null && currentRedeInPrefs != redeSelecionada) {
            redeSelecionada = currentRedeInPrefs
            // --- CORREÇÃO DE WARNING (String literal) ---
            textRedeAtual.text = getString(R.string.rede_dashboard_label, redeSelecionada)
            // Re-busca o papel para a nova rede selecionada, se necessário
            auth.currentUser?.uid?.let { uid ->
                firestore.collection("usuarios").document(uid).get()
                    .addOnSuccessListener { document ->
                        @Suppress("UNCHECKED_CAST")
                        val funcoes = document.get("funcoes") as? HashMap<String, String>
                        papelUsuarioLogado = funcoes?.get(redeSelecionada)
                        if (papelUsuarioLogado == null) {
                            papelUsuarioLogado = funcoes?.get("geral")
                        }
                        Log.d("PastorDashboard", "Papel atualizado para $papelUsuarioLogado na rede $redeSelecionada")
                    }
            }
            Toast.makeText(this, getString(R.string.dados_atualizados_rede, redeSelecionada), Toast.LENGTH_SHORT).show()
            // Chame aqui a função para recarregar dados específicos do Pastor, se houver
            // carregarDadosEstatisticosPastor()
        }
    }

    private fun setupNavigationAndProfileButton() {
        val profileImage = findViewById<ImageView>(R.id.image_profile)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.navigation_home

        profileImage.setOnClickListener {
            abrirSeletorDePerfil()
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    Toast.makeText(this, getString(R.string.clicou_em_inicio), Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.navigation_reports -> {
                    Toast.makeText(this, getString(R.string.clicou_em_relatorios), Toast.LENGTH_SHORT).show()
                    // TODO: Pastor Dashboard pode ter uma tela de relatórios diferente
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
                if (rede != redeSelecionada) {
                    val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    // --- CORREÇÃO DE WARNING (KTX extension) ---
                    sharedPref.edit {
                        putString("REDE_SELECIONADA", rede)
                    }
                    this.redeSelecionada = rede
                    // --- CORREÇÃO DE WARNING (String literal) ---
                    textRedeAtual.text = getString(R.string.rede_dashboard_label, redeSelecionada)
                    Toast.makeText(this, getString(R.string.exibindo_dados_rede_pastor, rede), Toast.LENGTH_SHORT).show()
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