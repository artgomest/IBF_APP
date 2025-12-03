// Em app/src/main/java/com/ibf/app/ui/usuarios/fragments/UsuariosAtivosFragment.kt

package com.ibf.app.ui.usuarios.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.ibf.app.R
import com.ibf.app.adapters.UsuarioRedeAdapter
import com.ibf.app.data.models.UsuarioRede
import com.ibf.app.ui.usuarios.DetalhesUsuarioActivity

class UsuariosAtivosFragment : Fragment(), UsuarioRedeAdapter.OnItemClickListener {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: UsuarioRedeAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private val listaUsuarios = mutableListOf<UsuarioRede>()

    private var redeId: String? = null
    private var papelUsuarioLogado: String? = null

    companion object {
        private const val ARG_REDE_ID = "rede_id"
        private const val ARG_PAPEL_LOGADO = "papel_logado"
        fun newInstance(redeId: String, papelLogado: String): UsuariosAtivosFragment {
            val fragment = UsuariosAtivosFragment()
            val args = Bundle()
            args.putString(ARG_REDE_ID, redeId)
            args.putString(ARG_PAPEL_LOGADO, papelLogado)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            redeId = it.getString(ARG_REDE_ID)
            papelUsuarioLogado = it.getString(ARG_PAPEL_LOGADO)
        }
        firestore = FirebaseFirestore.getInstance()
    }

    // onResume para atualizar a lista quando voltar da tela de detalhes
    override fun onResume() {
        super.onResume()
        // Recarrega sempre para garantir dados frescos
        carregarUsuariosDaRede()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_usuarios_ativos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view_usuarios_ativos)
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout_ativos)

        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = UsuarioRedeAdapter(listaUsuarios, this)
        recyclerView.adapter = adapter

        swipeRefreshLayout.setOnRefreshListener {
            carregarUsuariosDaRede()
        }
        
        // A chamada inicial já será feita pelo onResume
    }

    private fun carregarUsuariosDaRede() {
        // Evita chamadas múltiplas desnecessárias ou se a view não estiver pronta
        if (redeId == null) { 
            Log.e("UsuariosAtivosFragment", "ID da Rede não fornecido.")
            swipeRefreshLayout.isRefreshing = false
            return 
        }

        swipeRefreshLayout.isRefreshing = true

        firestore.collection("usuarios")
            .whereArrayContains("redes", redeId!!)
            .get()
            .addOnSuccessListener { documents ->
                val usuariosDaRede = mutableListOf<UsuarioRede>()
                for (document in documents) {
                    val uid = document.id
                    val nome = document.getString("nome") ?: "Nome Desconhecido"
                    
                    // Correção de Cast Seguro
                    val funcoesRaw = document.get("funcoes") as? Map<*, *>
                    val papelNaRede = funcoesRaw?.get(redeId)?.toString()

                    if (papelNaRede != null) {
                        usuariosDaRede.add(UsuarioRede(uid, nome, papelNaRede))
                    }
                }
                adapter.atualizarLista(usuariosDaRede.sortedBy { it.nome })
                swipeRefreshLayout.isRefreshing = false
            }
            .addOnFailureListener { e ->
                Log.e("UsuariosAtivosFragment", "Falha ao carregar usuários: ${e.message}", e)
                swipeRefreshLayout.isRefreshing = false
            }
    }

    override fun onItemClick(usuario: UsuarioRede) {
        val intent = Intent(context, DetalhesUsuarioActivity::class.java).apply {
            putExtra("USUARIO_ID", usuario.uid)
            putExtra("REDE_ID", redeId)
            putExtra("PAPEL_USUARIO_LOGADO", papelUsuarioLogado)
        }
        startActivity(intent)
    }
}