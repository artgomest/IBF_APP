package com.ibf.app

import android.os.Bundle
import android.util.Log
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

fun String.toDate(format: String = "yyyy-MM-dd"): Date? {
    return try {
        // SimpleDateFormat não é thread-safe, então crie uma nova instância a cada vez.
        // Embora para esta aplicação, o impacto seja mínimo, é uma boa prática.
        SimpleDateFormat(format, Locale.getDefault()).parse(this)
    } catch (e: Exception) {
        // Loga o erro para depuração, mas não quebra a aplicação.
        Log.e("DateConverter", "Erro ao converter data '$this' com formato '$format': ${e.message}")
        null
    }
}

class LiderGraficosActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var redeSelecionada: String? = null

    // Componentes do layout
    private lateinit var chartPessoas: BarChart
    private lateinit var chartOfertas: BarChart
    private lateinit var textMedia: TextView
    private lateinit var textVisitantes: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lider_graficos)

        redeSelecionada = intent.getStringExtra("REDE_SELECIONADA")

        if (redeSelecionada == null) {
            Toast.makeText(this, "Erro: Rede não especificada.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        chartPessoas = findViewById(R.id.chart_pessoas_por_culto)
        chartOfertas = findViewById(R.id.chart_ofertas_por_culto)
        textMedia = findViewById(R.id.text_media_valor)
        textVisitantes = findViewById(R.id.text_visitantes_valor)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)

        findViewById<ImageView>(R.id.button_back).setOnClickListener {
            finish()
        }

        swipeRefreshLayout.setOnRefreshListener {
            carregarDadosParaGraficos()
        }

        carregarDadosParaGraficos()
    }

    private fun carregarDadosParaGraficos() {
        swipeRefreshLayout.isRefreshing = true
        val redeId = redeSelecionada ?: return

        db.collection("relatorios")
            .whereEqualTo("idRede", redeId)
            .orderBy("dataReuniao", Query.Direction.DESCENDING) // Usando o campo 'data' para o qual criamos o índice
            .limit(8)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Nenhum relatório encontrado para esta rede.", Toast.LENGTH_SHORT).show()
                    chartPessoas.clear()
                    chartOfertas.clear()
                    textVisitantes.text = "0"
                    textMedia.text = "0"
                } else {
                    // Mapeia os documentos para a nossa data class Relatorio
                    val relatorios = documents.map { it.toObject(Relatorio::class.java) }.sortedBy { it.data.toDate() }

                    // --- CORREÇÃO APLICADA AQUI ---
                    val totalDeVisitantes = relatorios.sumOf { it.totalVisitantes } // Usando a variável correta
                    val mediaPessoas = if (relatorios.isNotEmpty()) relatorios.sumOf { it.totalPessoas } / relatorios.size else 0

                    textVisitantes.text = totalDeVisitantes.toString()
                    textMedia.text = mediaPessoas.toString()

                    configurarGraficoPessoas(relatorios)
                    configurarGraficoOfertas(relatorios)
                }

                swipeRefreshLayout.isRefreshing = false
            }
            .addOnFailureListener { exception ->
                Log.e("GraficosDebug", "Falha ao carregar dados: ", exception)
                Toast.makeText(this, "Falha ao carregar dados: ${exception.message}", Toast.LENGTH_LONG).show()
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
        estilizarGraficoDeBarras(chartPessoas, relatorios.map { it.dataReuniao.toDate() })
        chartPessoas.invalidate()
    }

    private fun configurarGraficoOfertas(relatorios: List<Relatorio>) {
        // Usando a variável correta 'valorOferta'
        val entries = relatorios.mapIndexed { index, relatorio ->
            BarEntry(index.toFloat(), relatorio.valorOferta.toFloat())
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