package com.ibf.app.ui.usuarios

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.ibf.app.R
import com.ibf.app.adapters.UsuarioRedeAdapter
import com.ibf.app.data.models.UsuarioRede

class MembrosRedeActivity : AppCompatActivity(), UsuarioRedeAdapter.OnItemClickListener {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: UsuarioRedeAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var textResumoMembros: TextView
    private lateinit var inputBuscaMembro: TextInputEditText
    private lateinit var emptyStateContainer: View
    private lateinit var emptyStateMessage: TextView
    private lateinit var emptyStateButton: MaterialButton
    private val listaUsuarios = mutableListOf<UsuarioRede>()
    private val todosUsuarios = mutableListOf<UsuarioRede>()

    private var redeSelecionada: String? = null
    private var papelUsuarioLogado: String? = null
    private var termoBuscaAtual: String = ""

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
        textResumoMembros = findViewById(R.id.text_total_membros)
        inputBuscaMembro = findViewById(R.id.edit_text_buscar_membro)
        emptyStateContainer = findViewById(R.id.layout_empty_state)
        emptyStateMessage = findViewById(R.id.text_empty_state_message)
        emptyStateButton = findViewById(R.id.button_empty_state_action)

        setupRecyclerView()
        configurarBotoes()
        configurarBusca()
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
            abrirTelaCadastroMembro()
        }

        findViewById<ImageView>(R.id.button_ver_agenda).setOnClickListener {
            Toast.makeText(this, "Tela de agenda de discipulados em breve!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun configurarBusca() {
        inputBuscaMembro.doOnTextChanged { text, _, _, _ ->
            termoBuscaAtual = text?.toString().orEmpty()
            aplicarFiltro()
        }

        emptyStateButton.setOnClickListener {
            if (termoBuscaAtual.isNotBlank()) {
                inputBuscaMembro.setText("")
            } else {
                abrirTelaCadastroMembro()
            }
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
                todosUsuarios.clear()
                todosUsuarios.addAll(usuariosDaRede.sortedBy { it.nome })
                aplicarFiltro()
                swipeRefreshLayout.isRefreshing = false // Para a animação de refresh
            }
            .addOnFailureListener { e ->
                Log.e("MembrosRedeActivity", "Falha ao carregar membros: ${e.message}", e)
                Toast.makeText(this, "Erro ao carregar membros.", Toast.LENGTH_SHORT).show()
                swipeRefreshLayout.isRefreshing = false // Para a animação de refresh também em caso de erro
            }
    }

    private fun aplicarFiltro() {
        val termo = termoBuscaAtual.trim()
        val filtroAtivo = termo.isNotEmpty()
        val listaFiltrada = if (filtroAtivo) {
            todosUsuarios.filter { usuario ->
                usuario.nome.contains(termo, ignoreCase = true) ||
                    usuario.papel.contains(termo, ignoreCase = true)
            }
        } else {
            todosUsuarios
        }

        adapter.atualizarLista(listaFiltrada)
        atualizarResumoResultados(listaFiltrada.size)
        atualizarEstadoVazio(filtroAtivo, listaFiltrada.isEmpty())
    }

    private fun atualizarResumoResultados(qtdMostrada: Int) {
        textResumoMembros.text = getString(R.string.total_membros_resumo, qtdMostrada, todosUsuarios.size)
    }

    private fun atualizarEstadoVazio(filtroAtivo: Boolean, deveMostrar: Boolean) {
        if (deveMostrar) {
            emptyStateContainer.visibility = View.VISIBLE
            emptyStateMessage.text = if (filtroAtivo) {
                getString(R.string.nenhum_membro_encontrado_busca)
            } else {
                getString(R.string.nenhum_membro_cadastrado)
            }
            emptyStateButton.text = if (filtroAtivo) {
                getString(R.string.limpar_filtro)
            } else {
                getString(R.string.cadastrar_primeiro_membro)
            }
        } else {
            emptyStateContainer.visibility = View.GONE
        }
    }

    private fun abrirTelaCadastroMembro() {
        val intent = Intent(this, CadastroMembroActivity::class.java)
        intent.putExtra("REDE_SELECIONADA", redeSelecionada)
        startActivity(intent)
    }

    override fun onItemClick(usuario: UsuarioRede) {
        // Ação para abrir a "ficha" do membro, passando o seu ID
        val intent = Intent(this, FichaMembroActivity::class.java)
        intent.putExtra("MEMBRO_ID", usuario.uid)
        startActivity(intent)
    }
}

