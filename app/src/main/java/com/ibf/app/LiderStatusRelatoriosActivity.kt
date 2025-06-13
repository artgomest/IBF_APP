package com.ibf.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// A classe implementa o OnItemClickListener para saber quando um item da lista é clicado
class LiderStatusRelatoriosActivity : AppCompatActivity(), RelatorioAdapter.OnItemClickListener {

    // As variáveis que já conhecemos para a lista e para o Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var relatorioAdapter: RelatorioAdapter
    private val listaDeStatus = mutableListOf<StatusRelatorio>()

    // Variável para saber de qual rede carregar os relatórios
    private var redeSelecionada: String? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lider_status_relatorios)

        // Recebe o nome da rede que foi passado pela tela de Menu
        redeSelecionada = intent.getStringExtra("REDE_SELECIONADA")

        // Se, por algum motivo, a rede não for passada, fecha a tela por segurança
        if (redeSelecionada == null) {
            Toast.makeText(this, "Erro: Rede não especificada.", Toast.LENGTH_LONG).show()
            finish() // finish() fecha a atividade atual e volta para a anterior
            return
        }

        // Inicializa o Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Configura o botão de voltar
        val backButton = findViewById<ImageView>(R.id.button_back)
        backButton.setOnClickListener {
            finish() // Ação de fechar a tela
        }

        // Atualiza o título da página para incluir o nome da rede
        val title = findViewById<TextView>(R.id.text_page_title)
        title.text = "Relatórios"

        // Chama as funções para configurar a lista e carregar os dados
        setupRecyclerView()
        carregarStatusDosRelatorios()
    }

    // Função para configurar a RecyclerView (exatamente a mesma que tínhamos)
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewRelatorios) // ID do nosso novo layout
        recyclerView.layoutManager = LinearLayoutManager(this)
        // Passamos 'this' como listener para que o onItemClick funcione
        relatorioAdapter = RelatorioAdapter(listaDeStatus, this)
        recyclerView.adapter = relatorioAdapter
    }

    // A nossa função principal para carregar os dados (exatamente a mesma que tínhamos)
    @SuppressLint("NotifyDataSetChanged")
    private fun carregarStatusDosRelatorios() {
        val redeAtiva = redeSelecionada ?: return
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        firestore.collection("redes").whereEqualTo("nome", redeAtiva).get().addOnSuccessListener { redesDocs ->
            if (redesDocs.isEmpty) return@addOnSuccessListener
            val diaDaSemana = redesDocs.documents.first().getLong("diaDaSemana")?.toInt() ?: return@addOnSuccessListener

            firestore.collection("relatorios")
                .whereEqualTo("idRede", redeAtiva)
                .get().addOnSuccessListener { relatoriosDocs ->
                    val relatoriosEnviados = relatoriosDocs.mapNotNull { doc -> doc.toObject(Relatorio::class.java).apply { id = doc.id } }
                    val statusFinal = mutableListOf<StatusRelatorio>()
                    val semanasParaVerificar = 8

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
                }.addOnFailureListener { e -> Log.e("FirestoreError", "Falha ao buscar relatorios", e) }
        }.addOnFailureListener { e -> Log.e("FirestoreError", "Falha ao buscar redes", e) }
    }

    // O que acontece quando um item da lista é clicado
    override fun onItemClick(status: StatusRelatorio) {
        // Usar 'when' é a forma ideal de tratar os diferentes tipos de StatusRelatorio
        when (status){
            is StatusRelatorio.Enviado -> {
                // Dentro deste bloco, 'status' é tratado como StatusRelatorio.Enviado
                Toast.makeText(this, "Visualizando detalhes do relatório de ${status.relatorio.dataReuniao}", Toast.LENGTH_SHORT).show()
            }
            is StatusRelatorio.Faltante -> {
                // Dentro deste bloco, 'status' é tratado como StatusRelatorio.Faltante
                Toast.makeText(this, "Relatório pendente para a data ${status.dataEsperada}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}