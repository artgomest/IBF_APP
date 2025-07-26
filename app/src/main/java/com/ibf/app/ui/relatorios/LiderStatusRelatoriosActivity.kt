package com.ibf.app.ui.relatorios

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
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

        redeSelecionada = redeInPrefs ?: intent.getStringExtra("REDE_SELECIONADA")

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
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val currentRedeInPrefs = sharedPref.getString("REDE_SELECIONADA", null)

        if (currentRedeInPrefs != null && currentRedeInPrefs != redeSelecionada) {
            redeSelecionada = currentRedeInPrefs
            textPageTitle.text = getString(R.string.relatorios_rede_label, redeSelecionada)
            carregarStatusDosRelatorios()
            Toast.makeText(this, getString(R.string.relatorios_atualizados_rede, redeSelecionada), Toast.LENGTH_SHORT).show()
        } else {
            carregarStatusDosRelatorios()
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewRelatorios)
        recyclerView.layoutManager = LinearLayoutManager(this)
        relatorioAdapter = RelatorioAdapter(listaDeStatus, this)
        recyclerView.adapter = relatorioAdapter
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun carregarStatusDosRelatorios() {
        val redeAtiva = redeSelecionada ?: run {
            Log.e("LiderRelatorios", "redeSelecionada é nula em carregarStatusDosRelatorios()")
            swipeRefreshLayout.isRefreshing = false
            return
        }
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        firestore.collection("redes").whereEqualTo("nome", redeAtiva).get().addOnSuccessListener { redesDocs ->
            if (redesDocs.isEmpty) {
                Log.e("FirestoreError", "Nenhuma rede encontrada com o nome: $redeAtiva")
                listaDeStatus.clear()
                relatorioAdapter.notifyDataSetChanged()
                return@addOnSuccessListener
            }
            val diaDaSemana = redesDocs.documents.first().getLong("diaDaSemana")?.toInt() ?: run {
                Log.e("FirestoreError", "Dia da semana não encontrado para a rede: $redeAtiva")
                return@addOnSuccessListener
            }

            firestore.collection("relatorios")
                .whereEqualTo("idRede", redeAtiva)
                .get().addOnSuccessListener { relatoriosDocs ->
                    val relatoriosEnviados = relatoriosDocs.mapNotNull { doc -> doc.toObject(Relatorio::class.java).apply { id = doc.id } }
                    val statusFinal = mutableListOf<StatusRelatorio>()
                    val semanasParaVerificar = 20

                    for (i in 0 until semanasParaVerificar) {
                        val dataEsperadaCal = Calendar.getInstance()
                        dataEsperadaCal.add(Calendar.WEEK_OF_YEAR, -i)
                        dataEsperadaCal.set(Calendar.DAY_OF_WEEK, diaDaSemana)

                        if (dataEsperadaCal.time.after(Date())) continue

                        val dataEsperadaStr = sdf.format(dataEsperadaCal.time)

                        val relatorioEncontrado = relatoriosEnviados.find { it.dataReuniao == dataEsperadaStr }
                        if (relatorioEncontrado != null) {
                            statusFinal.add(StatusRelatorio.Enviado(relatorioEncontrado))
                        } else {
                            statusFinal.add(StatusRelatorio.Faltante(dataEsperadaStr, redeAtiva))
                        }
                    }

                    listaDeStatus.clear()
                    listaDeStatus.addAll(statusFinal.sortedByDescending {
                        when(it) {
                            is StatusRelatorio.Enviado -> sdf.parse(it.relatorio.dataReuniao)
                            is StatusRelatorio.Faltante -> sdf.parse(it.dataEsperada)
                        }
                    })
                    relatorioAdapter.notifyDataSetChanged()
                    swipeRefreshLayout.isRefreshing = false
                }.addOnFailureListener { e -> Log.e("FirestoreError", "Falha ao buscar relatorios", e) }
                swipeRefreshLayout.isRefreshing = false
        }.addOnFailureListener { e -> Log.e("FirestoreError", "Falha ao buscar redes", e) }
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