package com.ibf.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class ListaUsuariosRedeActivity : AppCompatActivity(), UsuarioRedeAdapter.OnItemClickListener {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UsuarioRedeAdapter
    private val listaUsuarios = mutableListOf<UsuarioRede>()

    private var redeSelecionada: String? = null
    private var papelUsuarioLogado: String? = null // Para passar para a tela de cadastro

    private lateinit var textRedeUsuariosLista: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_usuarios_rede)

        firestore = FirebaseFirestore.getInstance()

        findViewById<TextView>(R.id.text_page_title).text = "Usuários da Rede"
        findViewById<ImageView>(R.id.button_back).setOnClickListener { finish() }

        textRedeUsuariosLista = findViewById(R.id.text_rede_usuarios_lista)
        recyclerView = findViewById(R.id.recycler_view_usuarios)
        val fabAdicionarUsuario = findViewById<FloatingActionButton>(R.id.fab_adicionar_usuario)

        redeSelecionada = intent.getStringExtra("REDE_SELECIONADA")
        papelUsuarioLogado = intent.getStringExtra("PAPEL_USUARIO_LOGADO")

        if (redeSelecionada == null || papelUsuarioLogado == null) {
            Toast.makeText(this, "Erro: Rede ou Papel do usuário não especificados.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        textRedeUsuariosLista.text = "Rede: $redeSelecionada"

        setupRecyclerView()

        // Botão FAB para adicionar novo usuário
        fabAdicionarUsuario.setOnClickListener {
            val papeisPermitidos = getPapeisPermitidosParaCadastro(papelUsuarioLogado!!)
            if (papeisPermitidos.isEmpty()) {
                Toast.makeText(this, "Você não tem permissão para cadastrar usuários nesta rede.", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, CadastroUsuarioActivity::class.java)
                intent.putExtra("REDE_SELECIONADA", redeSelecionada)
                intent.putExtra("PAPEL_USUARIO_LOGADO", papelUsuarioLogado) // Passa o papel para o CadastroUsuarioActivity
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Recarrega a lista toda vez que a Activity volta ao foco para refletir novas criações
        val currentRedeInPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("REDE_SELECIONADA", null)

        if (currentRedeInPrefs != null && currentRedeInPrefs != redeSelecionada) {
            redeSelecionada = currentRedeInPrefs
            textRedeUsuariosLista.text = "Rede: $redeSelecionada"
            Toast.makeText(this, "Lista de usuários atualizada para: $redeSelecionada", Toast.LENGTH_SHORT).show()
        }
        carregarUsuariosDaRede()
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = UsuarioRedeAdapter(listaUsuarios, this)
        recyclerView.adapter = adapter
    }

    private fun carregarUsuariosDaRede() {
        val redeId = redeSelecionada ?: return

        // Consulta otimizada:
        // Buscar usuários que contenham a 'redeId' no seu campo 'redes' (array)
        firestore.collection("usuarios")
            .whereArrayContains("redes", redeId) // <-- NOVO FILTRO AQUI
            .get()
            .addOnSuccessListener { documents ->
                val usuariosDaRede = mutableListOf<UsuarioRede>()
                for (document in documents) {
                    val uid = document.id
                    val nome = document.getString("nome") ?: "Nome Desconhecido"
                    @Suppress("UNCHECKED_CAST")
                    val funcoes = document.get("funcoes") as? HashMap<String, String>

                    // Verifica se este usuário tem uma função ESPECÍFICA para a rede selecionada
                    // e o usuário logado tem permissão para vê-lo.
                    // Esta verificação no cliente é um filtro adicional de segurança
                    // além das regras do Firestore.
                    val papelNaRede = funcoes?.get(redeId)
                    if (papelNaRede != null) {
                        // Opcional: Adicionar lógica para filtrar usuários que o Líder não deveria ver,
                        // por exemplo, Pastores que não pertencem à rede do Líder atual.
                        // Mas a regra do Firestore já cuidará disso.
                        usuariosDaRede.add(UsuarioRede(uid, nome, papelNaRede))
                    }
                }
                adapter.atualizarLista(usuariosDaRede.sortedBy { it.nome })
            }
            .addOnFailureListener { e ->
                Log.e("ListaUsuarios", "Erro ao carregar usuários: ${e.message}", e)
                Toast.makeText(this, "Falha ao carregar usuários: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Retorna a lista de papéis que o usuário logado PODE CADASTRAR.
    // Usado para verificar a permissão do botão FAB.
    private fun getPapeisPermitidosParaCadastro(papelLogado: String): List<String> {
        return when (papelLogado) {
            "pastor" -> listOf("lider", "secretario")
            "lider" -> listOf("secretario")
            else -> emptyList() // Secretário e outros não podem cadastrar
        }
    }

    override fun onItemClick(usuario: UsuarioRede) {
        // TODO: Implementar ação de clique no usuário (ex: ver detalhes, editar, remover)
        Toast.makeText(this, "Clicou em ${usuario.nome}, Papel: ${usuario.papel}", Toast.LENGTH_SHORT).show()
    }
}