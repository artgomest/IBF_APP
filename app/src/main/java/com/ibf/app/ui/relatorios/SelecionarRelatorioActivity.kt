package com.ibf.app.ui.relatorios

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ibf.app.R

class SelecionarRelatorioActivity : AppCompatActivity() { // Assumindo que esta é uma Activity, não um Sheet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Este layout pode não ser o bottom_sheet_selecionar_relatorio.xml.
        // Se esta é uma Activity, ela deveria ter seu próprio layout: activity_selecionar_relatorio.xml
        setContentView(R.layout.activity_selecionar_relatorio) // Garanta que este layout existe

        findViewById<TextView>(R.id.text_page_title)?.text = getString(R.string.selecionar_tipo_relatorio) // Título da Activity
        findViewById<ImageView>(R.id.button_back)?.setOnClickListener { finish() } // Botão de voltar

        val buttonVerRelatorios: Button = findViewById(R.id.button_ver_relatorios)
        val buttonCriarRelatorio: Button = findViewById(R.id.button_criar_relatorio)

        buttonVerRelatorios.setOnClickListener {
            Toast.makeText(this, getString(R.string.ver_relatorios_button), Toast.LENGTH_SHORT).show()
            // Exemplo de navegação para a lista de status de relatórios
            val intent = Intent(this, LiderStatusRelatoriosActivity::class.java)
            // Passe redeSelecionada aqui se esta activity precisar dela para filtrar a lista.
            startActivity(intent)
            finish()
        }

        buttonCriarRelatorio.setOnClickListener {
            Toast.makeText(this, getString(R.string.criar_novo_relatorio_button), Toast.LENGTH_SHORT).show()
            // Exemplo de navegação para o formulário de relatório
            val intent = Intent(this, FormularioRedeActivity::class.java)
            // Passe redeSelecionada e dataPendente se necessário para o formulário
            startActivity(intent)
            finish()
        }
    }
}