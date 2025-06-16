// Em app/src/main/java/com/ibf/app/ui/usuarios/fragments/SolicitacoesPendentesFragment.kt

package com.ibf.app.ui.usuarios.fragments

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
import com.google.firebase.firestore.Query
import com.ibf.app.R
import com.ibf.app.adapters.SolicitacoesAdapter
import com.ibf.app.data.models.SolicitacaoCadastro

class SolicitacoesPendentesFragment : Fragment(), SolicitacoesAdapter.SolicitacaoClickListener {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SolicitacoesAdapter
    private val listaSolicitacoes = mutableListOf<SolicitacaoCadastro>()

    private var redeId: String? = null

    companion object {
        private const val ARG_REDE_ID = "rede_id"
        fun newInstance(redeId: String): SolicitacoesPendentesFragment {
            val fragment = SolicitacoesPendentesFragment()
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
        return inflater.inflate(R.layout.fragment_solicitacoes_pendentes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView(view)
        carregarSolicitacoes()
    }

    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.recycler_view_solicitacoes)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = SolicitacoesAdapter(listaSolicitacoes, this)
        recyclerView.adapter = adapter
    }

    private fun carregarSolicitacoes() {
        if (redeId == null) {
            Log.e("SolicitacoesFragment", "ID da Rede não fornecido.")
            return
        }

        firestore.collection("solicitacoesCadastro")
            .whereEqualTo("redeId", redeId)
            .whereEqualTo("status", "pendente")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val novasSolicitacoes = documents.map { doc ->
                    doc.toObject(SolicitacaoCadastro::class.java).apply { id = doc.id }
                }
                adapter.atualizarLista(novasSolicitacoes)
                if (novasSolicitacoes.isEmpty()) {
                    Toast.makeText(context, "Nenhuma solicitação pendente.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("SolicitacoesFragment", "Erro ao carregar solicitações", e)
                Toast.makeText(context, "Erro ao carregar solicitações.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onAprovarClick(solicitacao: SolicitacaoCadastro) {
        Toast.makeText(context, "Aprovar: ${solicitacao.nome}", Toast.LENGTH_SHORT).show()
        // A lógica de aprovação no Firestore virá no próximo passo!
    }

    override fun onRejeitarClick(solicitacao: SolicitacaoCadastro) {
        Toast.makeText(context, "Rejeitar: ${solicitacao.nome}", Toast.LENGTH_SHORT).show()
        // A lógica de rejeição no Firestore virá no próximo passo!
    }
}