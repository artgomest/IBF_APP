// Em app/src/main/java/com/ibf/app/ui/usuarios/fragments/SolicitacoesPendentesFragment.kt

package com.ibf.app.ui.usuarios.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ibf.app.R
import com.ibf.app.adapters.SolicitacoesAdapter
import com.ibf.app.data.models.SolicitacaoCadastro

class SolicitacoesPendentesFragment : Fragment(), SolicitacoesAdapter.SolicitacaoClickListener {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: SolicitacoesAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
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

        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view_solicitacoes)
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout_solicitacoes)

        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = SolicitacoesAdapter(listaSolicitacoes, this)
        recyclerView.adapter = adapter

        swipeRefreshLayout.setOnRefreshListener {
            carregarSolicitacoes()
        }

        carregarSolicitacoes()
    }

    private fun carregarSolicitacoes() {
        swipeRefreshLayout.isRefreshing = true
        if (redeId == null) { Log.e("SolicitacoesFragment", "ID da Rede não fornecido.");
            swipeRefreshLayout.isRefreshing = false
            return }

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
                if (novasSolicitacoes.isEmpty() && context != null) {
                    Toast.makeText(context, "Nenhuma solicitação pendente.", Toast.LENGTH_SHORT).show()
                }
                swipeRefreshLayout.isRefreshing = false
            }
            .addOnFailureListener { e ->
                Log.e("SolicitacoesFragment", "Erro ao carregar solicitações", e)
                if (context != null) {
                    Toast.makeText(context, "Erro ao carregar solicitações: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                swipeRefreshLayout.isRefreshing = false
            }
    }

    /**
     * LÓGICA DE APROVAÇÃO
     * Cria o usuário na coleção 'usuarios' e remove a solicitação, tudo em uma única transação.
     */
    override fun onAprovarClick(solicitacao: SolicitacaoCadastro) {
        val batch = firestore.batch()

        // 1. Prepara a criação do novo documento na coleção 'usuarios'
        val novoUsuarioRef = firestore.collection("usuarios").document(solicitacao.uid)
        val funcoesMap = hashMapOf(solicitacao.redeId to solicitacao.papelSolicitado)
        val userData = hashMapOf(
            "nome" to solicitacao.nome,
            "email" to solicitacao.email,
            "funcoes" to funcoesMap,
            "redes" to listOf(solicitacao.redeId)
        )
        batch.set(novoUsuarioRef, userData)

        // 2. Prepara a exclusão do documento da coleção 'solicitacoesCadastro'
        val solicitacaoRef = firestore.collection("solicitacoesCadastro").document(solicitacao.id)
        batch.delete(solicitacaoRef)

        // 3. Executa a transação em lote
        batch.commit().addOnSuccessListener {
            Toast.makeText(context, "${solicitacao.nome} aprovado com sucesso!", Toast.LENGTH_SHORT).show()
            carregarSolicitacoes() // Recarrega a lista para remover o item aprovado
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Falha ao aprovar: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("Aprovacao", "Erro ao executar o batch de aprovação", e)
        }
    }

    /**
     * LÓGICA DE REJEIÇÃO
     * Apenas remove a solicitação.
     */
    override fun onRejeitarClick(solicitacao: SolicitacaoCadastro) {
        // Adiciona um diálogo de confirmação para segurança
        AlertDialog.Builder(requireContext())
            .setTitle("Rejeitar Solicitação")
            .setMessage("Tem certeza que deseja rejeitar a solicitação de ${solicitacao.nome}? Esta ação não pode ser desfeita.")
            .setPositiveButton("Sim, Rejeitar") { _, _ ->
                val solicitacaoRef = firestore.collection("solicitacoesCadastro").document(solicitacao.id)
                solicitacaoRef.delete()
                    .addOnSuccessListener {
                        Toast.makeText(context, "Solicitação de ${solicitacao.nome} rejeitada.", Toast.LENGTH_SHORT).show()
                        carregarSolicitacoes() // Recarrega a lista
                        // Opcional: deletar o usuário do Firebase Auth para não deixar contas órfãs.
                        // Esta ação é mais complexa e exigiria uma Cloud Function para ser feita de forma segura.
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Falha ao rejeitar: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e("Rejeicao", "Erro ao deletar solicitação", e)
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}