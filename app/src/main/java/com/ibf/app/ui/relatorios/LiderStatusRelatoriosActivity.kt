package com.ibf.app.ui.relatorios

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ibf.app.R
import com.ibf.app.adapters.RelatorioAdapter
import com.ibf.app.data.models.Relatorio
import com.ibf.app.data.models.StatusRelatorio
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class LiderStatusRelatoriosActivity : AppCompatActivity(), RelatorioAdapter.OnItemClickListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore // Variável 'firestore' estava com 'var' indevido
    private lateinit var recyclerView: RecyclerView
    private lateinit var relatorioAdapter: RelatorioAdapter
    private val listaDeStatus = mutableListOf<StatusRelatorio>()
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private var redeSelecionada: String? = null
    private lateinit var textPageTitle: TextView

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lider_status_relatorios)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance() // Inicializa firestore

        findViewById<ImageView>(R.id.button_back).setOnClickListener {
            finish()
        }

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout_relatorios)
        swipeRefreshLayout.setOnRefreshListener {
            // A ação de arrastar vai simplesmente chamar nossa função de carregar dados
            carregarStatusDosRelatorios()
        }

        textPageTitle = findViewById(R.id.text_page_title)

        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val redeInPrefs = sharedPref.getString("REDE_SELECIONADA", null)

        // Prioriza o intent, depois tenta o cache (para evitar o bug do Admin pegando rede "geral" em vez da específica)
        redeSelecionada = intent.getStringExtra("REDE_SELECIONADA") ?: redeInPrefs

        if (redeSelecionada == null) {
            Toast.makeText(this, getString(R.string.erro_rede_nao_especificada), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        textPageTitle.text = getString(R.string.relatorios_rede_label, redeSelecionada)

        setupRecyclerView()
        carregarStatusDosRelatorios()
    }

    override fun onResume() {
        super.onResume()
        // Removido o update forçado pelo SharedPreferences no onResume para não sobrescrever o intent
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val currentRedeInPrefs = sharedPref.getString("REDE_SELECIONADA", null)
        
        // Se a rede no intent não existir, usamos o fallback do prefs
        if (redeSelecionada == null && currentRedeInPrefs != null) {
            redeSelecionada = currentRedeInPrefs
            textPageTitle.text = getString(R.string.relatorios_rede_label, redeSelecionada)
            Toast.makeText(this, getString(R.string.relatorios_atualizados_rede, redeSelecionada), Toast.LENGTH_SHORT).show()
        }
        
        carregarStatusDosRelatorios()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewRelatorios)
        recyclerView.layoutManager = LinearLayoutManager(this)
        relatorioAdapter = RelatorioAdapter(listaDeStatus, this)
        recyclerView.adapter = relatorioAdapter
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun carregarStatusDosRelatorios() {
        val usuarioAtual = auth.currentUser ?: return
        val redeAtiva = redeSelecionada ?: return
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // --- CORREÇÃO APLICADA AQUI ---
        // 1. Definimos a nossa data de início
        val dataInicio = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 1, 0, 0, 0)
        }.time

        if (redeAtiva == "geral" || redeAtiva == "TODAS") {
            // Se for visão geral, busca todos os relatórios
            firestore.collection("relatorios").get()
                .addOnSuccessListener { relatoriosDocs ->
                    val relatoriosEnviados = relatoriosDocs.mapNotNull { doc -> doc.toObject(Relatorio::class.java).apply { id = doc.id } }
                    val statusFinal = relatoriosEnviados.map { StatusRelatorio.Enviado(it) }
                    
                    listaDeStatus.clear()
                    val sortedList = statusFinal.sortedByDescending {
                        try {
                            when(it) {
                                is StatusRelatorio.Enviado -> sdf.parse(it.relatorio.dataReuniao)
                                is StatusRelatorio.Faltante -> sdf.parse(it.dataEsperada)
                            }
                        } catch (e: Exception) {
                            Date(0)
                        }
                    }
                    listaDeStatus.addAll(sortedList)
                    relatorioAdapter.notifyDataSetChanged()
                    swipeRefreshLayout.isRefreshing = false
                }
                .addOnFailureListener { e ->
                    swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(this, "Erro ao carregar relatórios: ${e.message}", Toast.LENGTH_LONG).show()
                }
            return
        }

        firestore.collection("redes").whereEqualTo("nome", redeAtiva).get()
            .addOnSuccessListener { redesDocs ->
                if (redesDocs.isEmpty) {
                    swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(this, "Rede '$redeAtiva' não encontrada. Configure a rede em Configurações.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                val diaDaSemana = redesDocs.documents.first().getLong("diaDaSemana")?.toInt()
                val horarioReuniao = redesDocs.documents.first().getString("horarioReuniao")
                if (diaDaSemana == null) {
                    swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(this, "Dia da semana não configurado para a rede '$redeAtiva'. Acesse Configurações → Editar Rede.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                firestore.collection("relatorios")
                    .whereEqualTo("idRede", redeAtiva)
                    .get()
                    .addOnSuccessListener { relatoriosDocs ->
                        val relatoriosEnviados = relatoriosDocs.mapNotNull { doc -> doc.toObject(Relatorio::class.java).apply { id = doc.id } }
                        val statusFinal = mutableListOf<StatusRelatorio>()
                        
                        val calLoop = Calendar.getInstance()
                        // Ajustar para o dia da semana da rede
                        calLoop.set(Calendar.DAY_OF_WEEK, diaDaSemana)
                        
                        // Ajustar o horário da reunião, se houver, para não cobrar antes da hora
                        if (!horarioReuniao.isNullOrEmpty()) {
                            try {
                                val parts = horarioReuniao.split(":")
                                if (parts.size == 2) {
                                    calLoop.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                                    calLoop.set(Calendar.MINUTE, parts[1].toInt())
                                    calLoop.set(Calendar.SECOND, 0)
                                    calLoop.set(Calendar.MILLISECOND, 0)
                                }
                            } catch (e: Exception) {}
                        } else {
                            // Sem horário definido, só cobra no final do dia
                            calLoop.set(Calendar.HOUR_OF_DAY, 23)
                            calLoop.set(Calendar.MINUTE, 59)
                            calLoop.set(Calendar.SECOND, 59)
                        }
                        
                        // Se a data ajustada for no futuro, voltar uma semana
                        if (calLoop.time.after(Date())) {
                            calLoop.add(Calendar.WEEK_OF_YEAR, -1)
                        }

                        while (!calLoop.time.before(dataInicio)) {
                            val dataEsperadaStr = sdf.format(calLoop.time)
                            val relatorioEncontrado = relatoriosEnviados.find { it.dataReuniao == dataEsperadaStr }

                            if (relatorioEncontrado != null) {
                                statusFinal.add(StatusRelatorio.Enviado(relatorioEncontrado))
                            } else {
                                statusFinal.add(StatusRelatorio.Faltante(dataEsperadaStr, redeAtiva))
                            }
                            
                            calLoop.add(Calendar.WEEK_OF_YEAR, -1)
                        }

                        listaDeStatus.clear()
                        val sortedList = statusFinal.sortedByDescending {
                            try {
                                when(it) {
                                    is StatusRelatorio.Enviado -> sdf.parse(it.relatorio.dataReuniao)
                                    is StatusRelatorio.Faltante -> sdf.parse(it.dataEsperada)
                                }
                            } catch (e: Exception) {
                                Date(0)
                            }
                        }
                        listaDeStatus.addAll(sortedList)
                        relatorioAdapter.notifyDataSetChanged()
                        swipeRefreshLayout.isRefreshing = false
                    }
                    .addOnFailureListener { e ->
                        swipeRefreshLayout.isRefreshing = false
                        Toast.makeText(this, "Erro ao carregar relatórios: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                swipeRefreshLayout.isRefreshing = false
                Toast.makeText(this, "Erro ao verificar a rede: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // --- CORREÇÃO AQUI: onitemClick para o líder ---
    override fun onItemClick(status: StatusRelatorio) {
        val intent = Intent(this, FormularioRedeActivity::class.java)

        // Passamos a rede selecionada para o formulário
        intent.putExtra("REDE_SELECIONADA", redeSelecionada)

        when (status) {
            is StatusRelatorio.Enviado -> {
                // Se clicou em um relatório JÁ ENVIADO, passa o ID para edição
                Toast.makeText(this, getString(R.string.editando_relatorio, status.relatorio.dataReuniao), Toast.LENGTH_SHORT).show()
                intent.putExtra("RELATORIO_ID", status.relatorio.id)
                intent.putExtra("DATA_PENDENTE", status.relatorio.dataReuniao) // Passa a data do relatório existente
            }
            is StatusRelatorio.Faltante -> {
                // Se clicou em um relatório PENDENTE, passa a data para preenchimento
                Toast.makeText(this, getString(R.string.preenchendo_relatorio_pendente, status.dataEsperada), Toast.LENGTH_SHORT).show()
                intent.putExtra("DATA_PENDENTE", status.dataEsperada)
                intent.putExtra("REDE_SELECIONADA", status.nomeRede) // Passa a rede específica da pendência
            }
        }
        startActivity(intent)
    }
}