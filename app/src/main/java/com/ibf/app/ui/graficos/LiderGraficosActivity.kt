package com.ibf.app.ui.graficos

import android.content.Context
import android.content.Intent
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

import com.ibf.app.R // Importação de R
import com.ibf.app.data.models.Relatorio // Importação do modelo Relatorio
import com.ibf.app.util.toDate // Importação da função de extensão toDate

class LiderGraficosActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var redeSelecionada: String? = null

    private lateinit var chartPessoas: BarChart
    private lateinit var chartOfertas: BarChart
    private lateinit var textMedia: TextView
    private lateinit var textVisitantes: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var textPageTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lider_graficos)

        chartPessoas = findViewById(R.id.chart_pessoas_por_culto)
        chartOfertas = findViewById(R.id.chart_ofertas_por_culto)
        textMedia = findViewById(R.id.text_media_valor)
        textVisitantes = findViewById(R.id.text_visitantes_valor)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        textPageTitle = findViewById(R.id.text_page_title)

        findViewById<ImageView>(R.id.button_back).setOnClickListener {
            finish()
        }

        swipeRefreshLayout.setOnRefreshListener {
            carregarDadosParaGraficos()
        }

        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val redeInPrefs = sharedPref.getString("REDE_SELECIONADA", null)
        redeSelecionada = redeInPrefs ?: intent.getStringExtra("REDE_SELECIONADA")

        if (redeSelecionada == null) {
            Toast.makeText(this, getString(R.string.erro_rede_nao_especificada_logout), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        textPageTitle.text = getString(R.string.graficos_rede_label, redeSelecionada)

        carregarDadosParaGraficos()
    }

    override fun onResume() {
        super.onResume()
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val currentRedeInPrefs = sharedPref.getString("REDE_SELECIONADA", null)

        if (currentRedeInPrefs != null && currentRedeInPrefs != redeSelecionada) {
            redeSelecionada = currentRedeInPrefs
            textPageTitle.text = getString(R.string.graficos_rede_label, redeSelecionada)
            carregarDadosParaGraficos()
        }
    }

    private fun carregarDadosParaGraficos() {
        swipeRefreshLayout.isRefreshing = true
        val redeId = redeSelecionada ?: run {
            Toast.makeText(this, getString(R.string.erro_rede_nao_especificada_carregar_dados), Toast.LENGTH_SHORT).show()
            swipeRefreshLayout.isRefreshing = false
            return
        }

        db.collection("relatorios")
            .whereEqualTo("idRede", redeId)
            .orderBy("dataReuniao", Query.Direction.DESCENDING)
            .limit(8)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, getString(R.string.nenhum_relatorio_encontrado_rede), Toast.LENGTH_SHORT).show()
                    chartPessoas.clear()
                    chartOfertas.clear()
                    textVisitantes.text = "0"
                    textMedia.text = "0"
                } else {
                    val relatorios = documents.map { it.toObject(Relatorio::class.java).apply { id = it.id } }
                        .filterNotNull()
                        .sortedBy { it.dataReuniao.toDate("dd/MM/yyyy") }

                    val totalDeVisitantes = relatorios.sumOf { it.totalVisitantes }
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
                Toast.makeText(this, getString(R.string.falha_carregar_dados_graficos, exception.message), Toast.LENGTH_LONG).show()
                swipeRefreshLayout.isRefreshing = false
            }
    }

    private fun configurarGraficoPessoas(relatorios: List<Relatorio>) {
        val entries = relatorios.mapIndexed { index, relatorio ->
            BarEntry(index.toFloat(), relatorio.totalPessoas.toFloat())
        }
        val dataSet = BarDataSet(entries, getString(R.string.pessoas_por_culto))
        dataSet.color = ContextCompat.getColor(this, R.color.design_default_color_primary)
        dataSet.valueTextColor = ContextCompat.getColor(this, R.color.dark_text_primary)
        dataSet.valueTextSize = 12f
        chartPessoas.data = BarData(dataSet)
        estilizarGraficoDeBarras(chartPessoas, relatorios.mapNotNull { it.dataReuniao.toDate("dd/MM/yyyy") })
        chartPessoas.invalidate()
    }

    private fun configurarGraficoOfertas(relatorios: List<Relatorio>) {
        val entries = relatorios.mapIndexed { index, relatorio ->
            BarEntry(index.toFloat(), relatorio.valorOferta.toFloat())
        }
        val dataSet = BarDataSet(entries, getString(R.string.ofertas_por_culto))
        dataSet.color = ContextCompat.getColor(this, R.color.design_default_color_secondary)
        dataSet.valueTextColor = ContextCompat.getColor(this, R.color.dark_text_primary)
        dataSet.valueTextSize = 12f
        chartOfertas.data = BarData(dataSet)
        estilizarGraficoDeBarras(chartOfertas, relatorios.mapNotNull { it.data.toDate("dd/MM/yyyy") })
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