package com.ibf.app.ui.usuarios

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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.ibf.app.R
import com.ibf.app.adapters.UsuarioRedeAdapter
import com.ibf.app.data.models.UsuarioRede

class MembrosRedeActivity : AppCompatActivity(), UsuarioRedeAdapter.OnItemClickListener {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: UsuarioRedeAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private val listaUsuarios = mutableListOf<UsuarioRede>()

    private var redeSelecionada: String? = null
    private var papelUsuarioLogado: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_membros_rede)

        firestore = FirebaseFirestore.getInstance()
        redeSelecionada = intent.getStringExtra("REDE_SELECIONADA")
        papelUsuarioLogado = intent.getStringExtra("PAPEL_USUARIO_LOGADO")

        if (redeSelecionada == null) {
            Toast.makeText(this, "Erro: Rede não especificada.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        findViewById<ImageView>(R.id.button_back).setOnClickListener { finish() }
        findViewById<TextView>(R.id.text_page_title).text = "Membros da $redeSelecionada"
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)

        setupRecyclerView()
        configurarBotoes()
        setupSwipeToRefresh()
    }

    override fun onResume() {
        super.onResume()
        carregarMembrosDaRede()
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view_membros)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = UsuarioRedeAdapter(listaUsuarios, this)
        recyclerView.adapter = adapter
    }

    private fun configurarBotoes() {
        findViewById<FloatingActionButton>(R.id.fab_adicionar_membro).setOnClickListener {
            val intent = Intent(this, CadastroMembroActivity::class.java)
            intent.putExtra("REDE_SELECIONADA", redeSelecionada)
            startActivity(intent)
        }

        findViewById<ImageView>(R.id.button_ver_agenda).setOnClickListener {
            val intent = Intent(this, com.ibf.app.ui.agenda.AgendaActivity::class.java)
            intent.putExtra("REDE_SELECIONADA", redeSelecionada)
            startActivity(intent)
        }
    }

    private fun setupSwipeToRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            Log.d("MembrosRedeActivity", "Refresh acionado.")
            carregarMembrosDaRede()
        }
    }

    private fun carregarMembrosDaRede() {
        if (redeSelecionada == null) {
            swipeRefreshLayout.isRefreshing = false
            return
        }

        firestore.collection("usuarios")
            .whereGreaterThan(com.google.firebase.firestore.FieldPath.of("funcoes", redeSelecionada!!), "")
            .get()
            .addOnSuccessListener { documents ->
                val usuariosDaRede = mutableListOf<UsuarioRede>()
                for (document in documents) {
                    val uid = document.id
                    val nome = document.getString("nome") ?: "Nome Desconhecido"

                    @Suppress("UNCHECKED_CAST")
                    val funcoes = document.get("funcoes") as? HashMap<String, String>
                    val papelNaRede = funcoes?.get(redeSelecionada)

                    if (papelNaRede != null) {
                        usuariosDaRede.add(UsuarioRede(uid, nome, papelNaRede))
                    }
                }
                adapter.atualizarLista(usuariosDaRede.sortedBy { it.nome })
                swipeRefreshLayout.isRefreshing = false // Para a animação de refresh
            }
            .addOnFailureListener { e ->
                Log.e("MembrosRedeActivity", "Falha ao carregar membros: ${e.message}", e)
                Toast.makeText(this, "Erro ao carregar membros.", Toast.LENGTH_SHORT).show()
                swipeRefreshLayout.isRefreshing = false // Para a animação de refresh também em caso de erro
            }
    }

    override fun onItemClick(usuario: UsuarioRede) {
        // Ação para abrir a "ficha" do membro, passando o seu ID
        val intent = Intent(this, FichaMembroActivity::class.java)
        intent.putExtra("MEMBRO_ID", usuario.uid)
        startActivity(intent)
    }
}

