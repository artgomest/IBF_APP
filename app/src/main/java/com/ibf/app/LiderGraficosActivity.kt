package com.ibf.app

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LiderGraficosActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var redeSelecionada: String? = null

    // Componentes do layout
    private lateinit var chartPessoas: BarChart
    private lateinit var chartOfertas: BarChart
    private lateinit var textMedia: TextView
    private lateinit var textVisitantes: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout // ADICIONADO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lider_graficos)

        redeSelecionada = intent.getStringExtra("REDE_SELECIONADA")

        if (redeSelecionada == null) {
            Toast.makeText(this, "Erro: Rede não especificada.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // --- Conecta as variáveis aos IDs do layout ---
        chartPessoas = findViewById(R.id.chart_pessoas_por_culto)
        chartOfertas = findViewById(R.id.chart_ofertas_por_culto)
        textMedia = findViewById(R.id.text_media_valor)
        textVisitantes = findViewById(R.id.text_visitantes_valor)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout) // ADICIONADO

        // Configura o botão de voltar
        findViewById<ImageView>(R.id.button_back).setOnClickListener {
            finish()
        }

        // --- Configura o "Puxar para Atualizar" ---
        swipeRefreshLayout.setOnRefreshListener {
            // Quando o usuário puxar, simplesmente carregamos os dados de novo
            carregarDadosParaGraficos()
        }

        // Carrega os dados pela primeira vez
        carregarDadosParaGraficos()
    }

    private fun carregarDadosParaGraficos() {
        // MOSTRA a animação de "carregando"
        swipeRefreshLayout.isRefreshing = true

        val redeId = redeSelecionada
        if (redeId == null) {
            Toast.makeText(this, "Erro: Rede não encontrada", Toast.LENGTH_SHORT).show()
            swipeRefreshLayout.isRefreshing = false // ESCONDE a animação em caso de erro
            return
        }

        db.collection("relatorios")
            .whereEqualTo("redeId", redeId)
            .orderBy("data", Query.Direction.DESCENDING)
            .limit(8)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Nenhum relatório encontrado para esta rede.", Toast.LENGTH_SHORT).show()
                    // Limpa dados antigos se não houver novos
                    chartPessoas.clear()
                    chartOfertas.clear()
                    textVisitantes.text = "0"
                    textMedia.text = "0"
                } else {
                    val relatorios = documents.map { it.toObject(Relatorio::class.java) }.sortedBy { it.data.toDate() }

                    val totalVisitantes = relatorios.sumOf { it.numeroVisitantes }
                    val mediaPessoas = if (relatorios.isNotEmpty()) relatorios.sumOf { it.totalPessoas } / relatorios.size else 0

                    textVisitantes.text = totalVisitantes.toString()
                    textMedia.text = mediaPessoas.toString()

                    configurarGraficoPessoas(relatorios)
                    configurarGraficoOfertas(relatorios)
                }

                // ESCONDE a animação após o sucesso
                swipeRefreshLayout.isRefreshing = false
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Falha ao carregar dados: ${exception.message}", Toast.LENGTH_LONG).show()
                // ESCONDE a animação após a falha
                swipeRefreshLayout.isRefreshing = false
            }
    }

    private fun configurarGraficoPessoas(relatorios: List<Relatorio>) {
        val entries = relatorios.mapIndexed { index, relatorio ->
            BarEntry(index.toFloat(), relatorio.totalPessoas.toFloat())
        }

        val dataSet = BarDataSet(entries, "Pessoas por Culto")
        dataSet.color = ContextCompat.getColor(this, R.color.design_default_color_primary)
        dataSet.valueTextColor = ContextCompat.getColor(this, R.color.dark_text_primary)
        dataSet.valueTextSize = 12f

        chartPessoas.data = BarData(dataSet)
        estilizarGraficoDeBarras(chartPessoas, relatorios.map { it.data.toDate() })
        chartPessoas.invalidate()
    }

    private fun configurarGraficoOfertas(relatorios: List<Relatorio>) {
        val entries = relatorios.mapIndexed { index, relatorio ->
            BarEntry(index.toFloat(), relatorio.totalOfertas.toFloat())
        }

        val dataSet = BarDataSet(entries, "Ofertas por Culto")
        dataSet.color = ContextCompat.getColor(this, R.color.design_default_color_secondary)
        dataSet.valueTextColor = ContextCompat.getColor(this, R.color.dark_text_primary)
        dataSet.valueTextSize = 12f

        chartOfertas.data = BarData(dataSet)
        estilizarGraficoDeBarras(chartOfertas, relatorios.map { it.data.toDate() })
        chartOfertas.invalidate()
    }

    private fun estilizarGraficoDeBarras(chart: BarChart, datas: List<Date>) {
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.setDrawBarShadow(false)

        val formatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                val format = SimpleDateFormat("dd/MM", Locale.getDefault())
                return if (value.toInt() >= 0 && value.toInt() < datas.size) {
                    format.format(datas[value.toInt()])
                } else {
                    ""
                }
            }
        }

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.textColor = ContextCompat.getColor(this, R.color.dark_text_secondary)
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = formatter

        val leftAxis = chart.axisLeft
        leftAxis.textColor = ContextCompat.getColor(this, R.color.dark_text_secondary)
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = ContextCompat.getColor(this, R.color.dark_divider)
        leftAxis.axisMinimum = 0f

        chart.axisRight.isEnabled = false
    }
}