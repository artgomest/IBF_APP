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
import com.google.firebase.firestore.Query
import com.ibf.app.R
import com.ibf.app.adapters.RelatorioAdapter
import com.ibf.app.data.models.Relatorio
import com.ibf.app.data.models.StatusRelatorio
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PastorRelatoriosActivity : AppCompatActivity(), RelatorioAdapter.OnItemClickListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var relatorioAdapter: RelatorioAdapter
    private val listaDeStatus = mutableListOf<StatusRelatorio>()
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private var redeSelecionada: String? = null
    private lateinit var textPageTitle: TextView

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lider_status_relatorios) // Reutilizando layout

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        findViewById<ImageView>(R.id.button_back).setOnClickListener {
            finish()
        }

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout_relatorios)
        swipeRefreshLayout.setOnRefreshListener {
            carregarRelatorios()
        }

        textPageTitle = findViewById(R.id.text_page_title)

        redeSelecionada = intent.getStringExtra("REDE_SELECIONADA")

        if (redeSelecionada == null) {
            Toast.makeText(this, getString(R.string.erro_rede_nao_especificada), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        textPageTitle.text = if (redeSelecionada == "Todas") {
            "Relatórios - Visão Geral"
        } else {
            getString(R.string.relatorios_rede_label, redeSelecionada)
        }

        setupRecyclerView()
        carregarRelatorios()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewRelatorios)
        recyclerView.layoutManager = LinearLayoutManager(this)
        relatorioAdapter = RelatorioAdapter(listaDeStatus, this)
        recyclerView.adapter = relatorioAdapter
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun carregarRelatorios() {
        val redeAtiva = redeSelecionada ?: return
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val query = if (redeAtiva == "Todas") {
            firestore.collection("relatorios")
        } else {
            firestore.collection("relatorios")
                .whereEqualTo("idRede", redeAtiva)
        }

        // Ordenar por data de criação ou data de reunião se possível
        // Como 'dataReuniao' é string no código original, a ordenação pode ser complicada via query direta.
        // Vamos buscar e ordenar em memória por enquanto, ou usar orderBy se houver timestamp.
        
        query.get().addOnSuccessListener { relatoriosDocs ->
            val relatoriosEnviados = relatoriosDocs.mapNotNull { doc -> doc.toObject(Relatorio::class.java).apply { id = doc.id } }
            
            val statusFinal = relatoriosEnviados.map { StatusRelatorio.Enviado(it) }

            listaDeStatus.clear()
            val sortedList = statusFinal.sortedByDescending {
                try {
                    sdf.parse(it.relatorio.dataReuniao)
                } catch (e: Exception) {
                    Date(0)
                }
            }
            listaDeStatus.addAll(sortedList)
            relatorioAdapter.notifyDataSetChanged()
            swipeRefreshLayout.isRefreshing = false
        }
        .addOnFailureListener {
            Toast.makeText(this, "Erro ao carregar relatórios.", Toast.LENGTH_SHORT).show()
            swipeRefreshLayout.isRefreshing = false
        }
    }

    override fun onItemClick(status: StatusRelatorio) {
        val intent = Intent(this, FormularioRedeActivity::class.java)

        // Passamos a rede selecionada para o formulário (ou a rede do relatório se for Geral)
        
        when (status) {
            is StatusRelatorio.Enviado -> {
                intent.putExtra("REDE_SELECIONADA", status.relatorio.idRede) // Usa a rede do relatório
                Toast.makeText(this, getString(R.string.editando_relatorio, status.relatorio.dataReuniao), Toast.LENGTH_SHORT).show()
                intent.putExtra("RELATORIO_ID", status.relatorio.id)
                intent.putExtra("DATA_PENDENTE", status.relatorio.dataReuniao)
                
                // IMPORTANTE: Adicionar flag ou lógica para modo "Visualização" se o Pastor não deve editar
                // Por enquanto, permite edição como o Líder, mas talvez o Pastor só devesse ver.
            }
            is StatusRelatorio.Faltante -> {
               // Pastor não deve ver Faltantes nesta view por enquanto
            }
        }
        startActivity(intent)
    }
}
