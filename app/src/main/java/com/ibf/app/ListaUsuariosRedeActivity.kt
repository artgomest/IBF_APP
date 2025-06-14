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
import com.google.firebase.firestore.FirebaseFirestore // Mantido para clareza

class ListaUsuariosRedeActivity : AppCompatActivity(), UsuarioRedeAdapter.OnItemClickListener {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UsuarioRedeAdapter
    private val listaUsuarios = mutableListOf<UsuarioRede>()

    private var redeSelecionada: String? = null
    private var papelUsuarioLogado: String? = null

    private lateinit var textRedeUsuariosLista: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_usuarios_rede)

        firestore = FirebaseFirestore.getInstance()

        findViewById<TextView>(R.id.text_page_title).text = getString(R.string.usuarios_da_rede)
        findViewById<ImageView>(R.id.button_back).setOnClickListener { finish() }

        textRedeUsuariosLista = findViewById(R.id.text_rede_usuarios_lista)
        recyclerView = findViewById(R.id.recycler_view_usuarios)
        val fabAdicionarUsuario = findViewById<FloatingActionButton>(R.id.fab_adicionar_usuario)

        redeSelecionada = intent.getStringExtra("REDE_SELECIONADA")
        papelUsuarioLogado = intent.getStringExtra("PAPEL_USUARIO_LOGADO")

        if (redeSelecionada == null || papelUsuarioLogado == null) {
            Toast.makeText(this, getString(R.string.erro_rede_ou_papel_nao_especificados), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // --- CORREÇÃO DE WARNING (String literal) ---
        textRedeUsuariosLista.text = getString(R.string.rede_usuarios_lista_label, redeSelecionada)

        setupRecyclerView()

        fabAdicionarUsuario.setOnClickListener {
            val papeisPermitidos = getPapeisPermitidosParaCadastro(papelUsuarioLogado!!)
            if (papeisPermitidos.isEmpty()) {
                Toast.makeText(this, getString(R.string.sem_permissao_cadastrar_usuarios), Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, CadastroUsuarioActivity::class.java)
                intent.putExtra("REDE_SELECIONADA", redeSelecionada)
                intent.putExtra("PAPEL_USUARIO_LOGADO", papelUsuarioLogado)
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val currentRedeInPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("REDE_SELECIONADA", null)

        if (currentRedeInPrefs != null && currentRedeInPrefs != redeSelecionada) {
            redeSelecionada = currentRedeInPrefs
            // --- CORREÇÃO DE WARNING (String literal) ---
            textRedeUsuariosLista.text = getString(R.string.rede_usuarios_lista_label, redeSelecionada)
            Toast.makeText(this, getString(R.string.lista_usuarios_atualizada_para, redeSelecionada), Toast.LENGTH_SHORT).show()
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

        firestore.collection("usuarios")
            .whereArrayContains("redes", redeId)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("ListaUsuarios", "Consulta bem-sucedida. Documentos encontrados: ${documents.size()}")
                val usuariosDaRede = mutableListOf<UsuarioRede>()
                if (documents.isEmpty) {
                    Toast.makeText(this, getString(R.string.nenhum_usuario_encontrado_para_rede, redeId), Toast.LENGTH_SHORT).show()
                    Log.d("ListaUsuarios", "Nenhum usuário encontrado para a rede: $redeId")
                }

                for (document in documents) {
                    val uid = document.id
                    val nome = document.getString("nome") ?: "Nome Desconhecido"
                    @Suppress("UNCHECKED_CAST")
                    val funcoes = document.get("funcoes") as? HashMap<String, String>

                    val papelNaRede = funcoes?.get(redeId)
                    if (papelNaRede != null) {
                        usuariosDaRede.add(UsuarioRede(uid, nome, papelNaRede))
                        Log.d("ListaUsuarios", "Adicionado usuário: $nome, Papel: $papelNaRede, UID: $uid")
                    } else {
                        Log.w("ListaUsuarios", "Usuário ${nome} (UID: $uid) encontrado para rede $redeId, mas sem papel definido para essa rede em 'funcoes'.")
                    }
                }
                adapter.atualizarLista(usuariosDaRede.sortedBy { it.nome })
                Log.d("ListaUsuarios", "Lista de usuários atualizada. Total de usuários exibidos: ${usuariosDaRede.size}")
            }
            .addOnFailureListener { e ->
                Log.e("ListaUsuarios", "Falha ao carregar usuários: ${e.message}", e)
                Toast.makeText(this, getString(R.string.falha_carregar_usuarios, e.message), Toast.LENGTH_SHORT).show()
            }
    }

    private fun getPapeisPermitidosParaCadastro(papelLogado: String): List<String> {
        return when (papelLogado) {
            "pastor" -> listOf("lider", "secretario")
            "lider" -> listOf("secretario")
            else -> emptyList()
        }
    }

    override fun onItemClick(usuario: UsuarioRede) {
        Toast.makeText(this, getString(R.string.clicou_em_usuario, usuario.nome, usuario.papel), Toast.LENGTH_SHORT).show()
    }
}