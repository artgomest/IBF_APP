package com.ibf.app

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

    // Variáveis do Firebase e da Rede
    private val db = FirebaseFirestore.getInstance()
    private var redeSelecionada: String? = null

    // Componentes do layout
    private lateinit var chartPessoas: BarChart
    private lateinit var chartOfertas: BarChart
    private lateinit var textMedia: TextView
    private lateinit var textVisitantes: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lider_graficos)

        redeSelecionada = intent.getStringExtra("REDE_SELECIONADA")

        if (redeSelecionada == null) {
            Toast.makeText(this, "Erro: Rede não especificada.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        findViewById<ImageView>(R.id.button_back).setOnClickListener {
            finish()
        }

        // Conecta as variáveis aos IDs do layout
        chartPessoas = findViewById(R.id.chart_pessoas_por_culto)
        chartOfertas = findViewById(R.id.chart_ofertas_por_culto)
        textMedia = findViewById(R.id.text_media_valor)
        textVisitantes = findViewById(R.id.text_visitantes_valor)

        // Inicia o carregamento dos dados
        carregarDadosParaGraficos()
    }

    private fun carregarDadosParaGraficos() {
        val redeId = redeSelecionada
        if (redeId == null) {
            Toast.makeText(this, "Erro: Rede não encontrada", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("relatorios")
            .whereEqualTo("redeId", redeId)
            .orderBy("data", Query.Direction.DESCENDING) // Pega os mais recentes primeiro
            .limit(8) // Limita aos últimos 8 relatórios
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Nenhum relatório encontrado para esta rede.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Converte os documentos do Firestore para uma lista de objetos Relatorio
                // e ordena pela data, do mais antigo para o mais novo, para o gráfico fazer sentido
                val relatorios = documents.map { it.toObject(Relatorio::class.java) }.sortedBy { it.data.toDate() }

                // Calcula as estatísticas
                val totalVisitantes = relatorios.sumOf { it.numeroVisitantes }
                val mediaPessoas = if (relatorios.isNotEmpty()) relatorios.sumOf { it.totalPessoas } / relatorios.size else 0

                // Atualiza os TextViews dos cards
                textVisitantes.text = totalVisitantes.toString()
                textMedia.text = mediaPessoas.toString()

                // Configura e exibe os gráficos
                configurarGraficoPessoas(relatorios)
                configurarGraficoOfertas(relatorios)

            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Falha ao carregar dados: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun configurarGraficoPessoas(relatorios: List<Relatorio>) {
        val entries = relatorios.mapIndexed { index, relatorio ->
            BarEntry(index.toFloat(), relatorio.totalPessoas.toFloat())
        }

        val dataSet = BarDataSet(entries, "Pessoas por Culto")
        dataSet.color = ContextCompat.getColor(this, R.color.design_default_color_primary) // Lembre-se de ter essa cor em colors.xml
        dataSet.valueTextColor = ContextCompat.getColor(this, R.color.dark_text_primary)
        dataSet.valueTextSize = 12f

        val barData = BarData(dataSet)
        chartPessoas.data = barData

        estilizarGraficoDeBarras(chartPessoas, relatorios.map { it.data.toDate() })
        chartPessoas.invalidate() // Atualiza o gráfico na tela
    }

    private fun configurarGraficoOfertas(relatorios: List<Relatorio>) {
        val entries = relatorios.mapIndexed { index, relatorio ->
            BarEntry(index.toFloat(), relatorio.totalOfertas.toFloat())
        }

        val dataSet = BarDataSet(entries, "Ofertas por Culto")
        dataSet.color = ContextCompat.getColor(this, R.color.design_default_color_secondary) // Lembre-se de ter essa cor em colors.xml
        dataSet.valueTextColor = ContextCompat.getColor(this, R.color.dark_text_primary)
        dataSet.valueTextSize = 12f

        val barData = BarData(dataSet)
        chartOfertas.data = barData

        estilizarGraficoDeBarras(chartOfertas, relatorios.map { it.data.toDate() })
        chartOfertas.invalidate()
    }

    private fun estilizarGraficoDeBarras(chart: BarChart, datas: List<Date>) {
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.setDrawBarShadow(false)

        // Formata o eixo X (inferior) para mostrar as datas
        val formatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                // Movemos a criação do SimpleDateFormat para dentro do método
                val format = SimpleDateFormat("dd/MM", Locale.getDefault()) // <--- CORREÇÃO
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