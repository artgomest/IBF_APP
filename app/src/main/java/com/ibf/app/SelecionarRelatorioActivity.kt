package com.ibf.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class SelecionarRelatorioActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selecionar_relatorio)

        val cardRede = findViewById<MaterialCardView>(R.id.card_relatorio_rede)
        val cardFinanceiro = findViewById<MaterialCardView>(R.id.card_relatorio_financeiro)

        // Abre o formulário de rede que já temos
        cardRede.setOnClickListener {
            startActivity(Intent(this, FormularioRedeActivity::class.java))
        }

        // Apenas mostra uma mensagem, pois ainda não criamos este formulário
        cardFinanceiro.setOnClickListener {
            Toast.makeText(this, "Função a ser implementada!", Toast.LENGTH_SHORT).show()
        }
    }
}