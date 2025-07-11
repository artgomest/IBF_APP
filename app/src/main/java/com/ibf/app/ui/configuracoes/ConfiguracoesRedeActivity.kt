package com.ibf.app.ui.configuracoes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.ibf.app.R // Importação de R
import com.ibf.app.ui.usuarios.ListaUsuariosRedeActivity // Importação da Activity de usuários

class ConfiguracoesRedeActivity : AppCompatActivity() {

    private var redeSelecionada: String? = null
    private var papelUsuarioLogado: String? = null
    private lateinit var textRedeAtualConfig: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuracoes_rede)

        findViewById<TextView>(R.id.text_page_title).text = getString(R.string.configuracoes_da_rede)
        findViewById<ImageView>(R.id.button_back).setOnClickListener { finish() }

        textRedeAtualConfig = findViewById(R.id.text_rede_atual_config)

        redeSelecionada = intent.getStringExtra("REDE_SELECIONADA")
        papelUsuarioLogado = intent.getStringExtra("PAPEL_USUARIO_LOGADO")

        if (redeSelecionada == null || papelUsuarioLogado == null) {
            Toast.makeText(this, getString(R.string.erro_rede_ou_papel_nao_especificados), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        textRedeAtualConfig.text = getString(R.string.rede_selecionada_label, redeSelecionada)

        findViewById<MaterialCardView>(R.id.card_usuarios).setOnClickListener {
            val intent = Intent(this, ListaUsuariosRedeActivity::class.java)
            intent.putExtra("REDE_SELECIONADA", redeSelecionada)
            intent.putExtra("PAPEL_USUARIO_LOGADO", papelUsuarioLogado)
            startActivity(intent)
        }

        findViewById<MaterialCardView>(R.id.card_rede_config).setOnClickListener {
            val intent = Intent(this, EditarRedeActivity::class.java)
            intent.putExtra("REDE_SELECIONADA", redeSelecionada)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val currentRedeInPrefs = sharedPref.getString("REDE_SELECIONADA", null)

        if (currentRedeInPrefs != null && currentRedeInPrefs != redeSelecionada) {
            redeSelecionada = currentRedeInPrefs
            textRedeAtualConfig.text = getString(R.string.rede_selecionada_label, redeSelecionada)
            Toast.makeText(this, getString(R.string.rede_atualizada_para, redeSelecionada), Toast.LENGTH_SHORT).show()
        }
    }
}