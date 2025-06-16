// Em app/src/main/java/com/ibf/app/ui/usuarios/fragments/UsuariosAtivosFragment.kt

package com.ibf.app.ui.usuarios

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.ibf.app.R
import com.ibf.app.adapters.UsuarioRedeAdapter
import com.ibf.app.data.models.UsuarioRede

class UsuariosAtivosFragment : Fragment(), UsuarioRedeAdapter.OnItemClickListener {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: UsuarioRedeAdapter
    private val listaUsuarios = mutableListOf<UsuarioRede>()

    private var redeId: String? = null

    companion object {
        private const val ARG_REDE_ID = "rede_id"
        fun newInstance(redeId: String): UsuariosAtivosFragment {
            val fragment = UsuariosAtivosFragment()
            val args = Bundle()
            args.putString(ARG_REDE_ID, redeId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            redeId = it.getString(ARG_REDE_ID)
        }
        firestore = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_usuarios_ativos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view_usuarios_ativos)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = UsuarioRedeAdapter(listaUsuarios, this)
        recyclerView.adapter = adapter
        carregarUsuariosDaRede()
    }

    private fun carregarUsuariosDaRede() {
        if (redeId == null) { Log.e("UsuariosAtivosFragment", "ID da Rede não fornecido."); return }

        firestore.collection("usuarios")
            .whereArrayContains("redes", redeId!!)
            .get()
            .addOnSuccessListener { documents ->
                val usuariosDaRede = mutableListOf<UsuarioRede>()
                for (document in documents) {
                    val uid = document.id
                    val nome = document.getString("nome") ?: "Nome Desconhecido"
                    @Suppress("UNCHECKED_CAST")
                    val funcoes = document.get("funcoes") as? HashMap<String, String>
                    val papelNaRede = funcoes?.get(redeId)
                    if (papelNaRede != null) {
                        usuariosDaRede.add(UsuarioRede(uid, nome, papelNaRede))
                    }
                }
                adapter.atualizarLista(usuariosDaRede.sortedBy { it.nome })
            }
            .addOnFailureListener { e ->
                Log.e("UsuariosAtivosFragment", "Falha ao carregar usuários: ${e.message}", e)
            }
    }

    override fun onItemClick(usuario: UsuarioRede) {
        Toast.makeText(context, "Clicou em ${usuario.nome}", Toast.LENGTH_SHORT).show()
        // No futuro, aqui você pode abrir uma tela de detalhes do usuário.
    }
}