package com.ibf.app.ui.dashboard // Seu package name correto

import android.content.Context
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
import com.ibf.app.R
import com.ibf.app.ui.main.MainActivity
import com.ibf.app.ui.shared.SelecionarPerfilSheet

class PastorDashboardActivity : AppCompatActivity(), SelecionarPerfilSheet.PerfilSelecionadoListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var redeSelecionada: String? = null
    private var papelUsuarioLogado: String? = null
    private lateinit var textRedeAtual: TextView
    private lateinit var greetingText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pastor_dashboard)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        textRedeAtual = findViewById(R.id.text_rede_dashboard)
        greetingText = findViewById(R.id.text_greeting)

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
                    val nome = document.getString("nome") ?: getString(R.string.pastor_padrao)
                    greetingText.text = getString(R.string.ola_pastor_nome, nome)

                    @Suppress("UNCHECKED_CAST")
                    val funcoes = document.get("funcoes") as? HashMap<String, String>
                    papelUsuarioLogado = funcoes?.get(redeSelecionada)
                    if (papelUsuarioLogado == null) {
                        papelUsuarioLogado = funcoes?.get("geral")
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

        setupNavigationAndProfileButton()

        // Aqui você chamaria funções para carregar quaisquer dados específicos do Pastor
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
                        Log.d("PastorDashboard", "Papel atualizado para $papelUsuarioLogado na rede $redeSelecionada")
                    }
            }
            Toast.makeText(this, getString(R.string.dados_atualizados_rede, redeSelecionada), Toast.LENGTH_SHORT).show()
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

    override fun onPerfilSelecionado(rede: String, papel: String) {
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
                    Toast.makeText(this, getString(R.string.exibindo_dados_rede_pastor, rede), Toast.LENGTH_SHORT).show()
                }
                return
            }

            intent.putExtra("REDE_SELECIONADA", rede)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onLogoutSelecionado() {
        fazerLogout()
    }
}