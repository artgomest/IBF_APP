// ConfiguracoesRedeActivity.kt

package com.ibf.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class ConfiguracoesRedeActivity : AppCompatActivity() {

    private var redeSelecionada: String? = null
    private var papelUsuarioLogado: String? = null // Papel do usuário que acessou esta tela
    private lateinit var textRedeAtualConfig: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuracoes_rede)

        findViewById<TextView>(R.id.text_page_title).text = getString(R.string.configuracoes_da_rede) // Usando string resource
        findViewById<ImageView>(R.id.button_back).setOnClickListener { finish() }

        textRedeAtualConfig = findViewById(R.id.text_rede_atual_config)

        redeSelecionada = intent.getStringExtra("REDE_SELECIONADA")
        papelUsuarioLogado = intent.getStringExtra("PAPEL_USUARIO_LOGADO")

        if (redeSelecionada == null || papelUsuarioLogado == null) {
            Toast.makeText(this, "Erro: Rede ou Papel do usuário não especificados.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        textRedeAtualConfig.text = getString(R.string.rede_selecionada_label, redeSelecionada) // Usando string resource

        // Configura o clique no card de USUÁRIOS
        findViewById<MaterialCardView>(R.id.card_usuarios).setOnClickListener {
            val intent = Intent(this, ListaUsuariosRedeActivity::class.java) // <--- Abre ListaUsuariosRedeActivity
            intent.putExtra("REDE_SELECIONADA", redeSelecionada)
            intent.putExtra("PAPEL_USUARIO_LOGADO", papelUsuarioLogado)
            startActivity(intent)
        }

        // Configura o clique no card de REDE
        findViewById<MaterialCardView>(R.id.card_rede_config).setOnClickListener {
            Toast.makeText(this, "Configurações de Rede em breve!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        val currentRedeInPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("REDE_SELECIONADA", null)

        if (currentRedeInPrefs != null && currentRedeInPrefs != redeSelecionada) {
            redeSelecionada = currentRedeInPrefs
            textRedeAtualConfig.text = getString(R.string.rede_selecionada_label, redeSelecionada)
            Toast.makeText(this, "Rede atualizada para: $redeSelecionada", Toast.LENGTH_SHORT).show()
        }
    }
}