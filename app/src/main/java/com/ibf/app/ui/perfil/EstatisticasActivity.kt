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
import com.ibf.app.data.models.Relatorio
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EstatisticasActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var textTotalEnviados: TextView
    private lateinit var textTaxaEntrega: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_estatisticas)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        textTotalEnviados = findViewById(R.id.text_total_enviados)
        textTaxaEntrega = findViewById(R.id.text_taxa_entrega)
        findViewById<ImageView>(R.id.button_back).setOnClickListener { finish() }

        carregarDadosEstatisticas()
    }

    private fun carregarDadosEstatisticas() {
        val usuarioAtual = auth.currentUser ?: return
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // Busca os dados do usuário para saber de quais redes ele é secretário
        firestore.collection("usuarios").document(usuarioAtual.uid).get().addOnSuccessListener { userDoc ->
            @Suppress("UNCHECKED_CAST")
            val funcoes = userDoc.get("funcoes") as? HashMap<String, String>
            val redesDoSecretario = funcoes?.filterValues { it == "secretario" }?.keys ?: setOf()

            if (redesDoSecretario.isEmpty()) {
                Toast.makeText(this, "Você não é secretário de nenhuma rede.", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            // Busca os dados de TODAS as redes para saber o dia da semana
            firestore.collection("redes").get().addOnSuccessListener { redesDocs ->
                val mapaRedes = redesDocs.documents.associate { it.getString("nome") to it.getLong("diaDaSemana")?.toInt() }

                // Busca TODOS os relatórios enviados por este secretário
                firestore.collection("relatorios").whereEqualTo("autorUid", usuarioAtual.uid).get().addOnSuccessListener { relatoriosDocs ->
                    val relatoriosEnviados = relatoriosDocs.mapNotNull { doc -> doc.toObject(Relatorio::class.java) }

                    // --- CÁLCULO DAS ESTATÍSTICAS ---

                    // 1. Total de Relatórios Enviados
                    val totalEnviados = relatoriosEnviados.size
                    textTotalEnviados.text = totalEnviados.toString()

                    // 2. Taxa de Entrega
                    var totalEsperado = 0
                    val semanasParaVerificar = 8
                    redesDoSecretario.forEach { nomeDaRede ->
                        val diaDaSemana = mapaRedes[nomeDaRede] ?: return@forEach
                        for (i in 0 until semanasParaVerificar) {
                            val dataEsperadaCal = Calendar.getInstance()
                            dataEsperadaCal.add(Calendar.WEEK_OF_YEAR, -i)
                            dataEsperadaCal.set(Calendar.DAY_OF_WEEK, diaDaSemana)
                            if (!dataEsperadaCal.time.after(Date())) {
                                totalEsperado++
                            }
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
        }.addOnFailureListener { e -> tratarFalha(e) }
    }

    private fun tratarFalha(exception: Exception) {
        Toast.makeText(this, "Erro ao calcular estatísticas.", Toast.LENGTH_SHORT).show()
        Log.e("EstatisticasError", "Falha: ", exception)
    }
}