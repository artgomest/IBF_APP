package com.ibf.app.ui.perfil

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ibf.app.R
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EstatisticasActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var textTotalEnviados: TextView
    private lateinit var textTaxaEntrega: TextView

    private var redeSelecionada: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_estatisticas)

        // Recebe a rede que foi passada pela tela de Perfil
        redeSelecionada = intent.getStringExtra("REDE_SELECIONADA")
        if (redeSelecionada == null) {
            Toast.makeText(this, "Erro: Nenhuma rede especificada para as estatísticas.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        textTotalEnviados = findViewById(R.id.text_total_enviados)
        textTaxaEntrega = findViewById(R.id.text_taxa_entrega)
        findViewById<ImageView>(R.id.button_back).setOnClickListener { finish() }

        carregarDadosEstatisticas()
    }

    private fun carregarDadosEstatisticas() {
        val usuarioAtual = auth.currentUser ?: return
        val redeAtiva = redeSelecionada ?: return

        // Busca os dados da rede ativa para saber o dia da semana
        firestore.collection("redes").whereEqualTo("nome", redeAtiva).get().addOnSuccessListener { redesDocs ->
            if (redesDocs.isEmpty) return@addOnSuccessListener
            val diaDaSemana = redesDocs.documents.first().getLong("diaDaSemana")?.toInt() ?: return@addOnSuccessListener

            // Busca os relatórios enviados por este secretário APENAS PARA A REDE ATIVA
            firestore.collection("relatorios")
                .whereEqualTo("autorUid", usuarioAtual.uid)
                .whereEqualTo("idRede", redeAtiva) // <-- FILTRO IMPORTANTE
                .get().addOnSuccessListener { relatoriosDocs ->

                    // 1. Total de Relatórios Enviados (para esta rede)
                    val totalEnviados = relatoriosDocs.size()
                    textTotalEnviados.text = totalEnviados.toString()

                    // 2. Taxa de Entrega (para esta rede)
                    var totalEsperado = 0
                    val semanasParaVerificar = 8
                    for (i in 0 until semanasParaVerificar) {
                        val dataEsperadaCal = Calendar.getInstance()
                        dataEsperadaCal.add(Calendar.WEEK_OF_YEAR, -i)
                        dataEsperadaCal.set(Calendar.DAY_OF_WEEK, diaDaSemana)
                        if (!dataEsperadaCal.time.after(Date())) {
                            totalEsperado++
                        }
                    }

                    if (totalEsperado > 0) {
                        val taxa = (totalEnviados.toDouble() / totalEsperado.toDouble()) * 100
                        textTaxaEntrega.text = String.format(Locale.getDefault(), "%.1f%%", taxa)
                    } else {
                        textTaxaEntrega.text = "N/A"
                    }

                }.addOnFailureListener { e -> tratarFalha(e) }
        }.addOnFailureListener { e -> tratarFalha(e) }
    }

    private fun tratarFalha(exception: Exception) {
        Toast.makeText(this, "Erro ao calcular estatísticas.", Toast.LENGTH_SHORT).show()
        Log.e("EstatisticasError", "Falha: ", exception)
    }
}