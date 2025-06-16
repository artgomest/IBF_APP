// Em app/src/main/java/com/ibf/app/adapters/SolicitacoesAdapter.kt

package com.ibf.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ibf.app.R
import com.ibf.app.data.models.SolicitacaoCadastro

class SolicitacoesAdapter(
    private val listaSolicitacoes: MutableList<SolicitacaoCadastro>,
    private val listener: SolicitacaoClickListener
) : RecyclerView.Adapter<SolicitacoesAdapter.SolicitacaoViewHolder>() {

    /**
     * Interface para comunicar os cliques nos botões para a Activity.
     * A Activity será responsável pela lógica de aprovar/rejeitar.
     */
    interface SolicitacaoClickListener {
        fun onAprovarClick(solicitacao: SolicitacaoCadastro)
        fun onRejeitarClick(solicitacao: SolicitacaoCadastro)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SolicitacaoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_solicitacao_pendente, parent, false)
        return SolicitacaoViewHolder(view)
    }

    override fun getItemCount() = listaSolicitacoes.size

    override fun onBindViewHolder(holder: SolicitacaoViewHolder, position: Int) {
        val solicitacao = listaSolicitacoes[position]
        holder.bind(solicitacao, listener)
    }

    fun atualizarLista(novaLista: List<SolicitacaoCadastro>) {
        listaSolicitacoes.clear()
        listaSolicitacoes.addAll(novaLista)
        notifyDataSetChanged()
    }

    class SolicitacaoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nomeSolicitante: TextView = itemView.findViewById(R.id.text_nome_solicitante)
        private val papelSolicitado: TextView = itemView.findViewById(R.id.text_papel_solicitado)
        private val botaoAprovar: Button = itemView.findViewById(R.id.button_aprovar)
        private val botaoRejeitar: Button = itemView.findViewById(R.id.button_rejeitar)

        fun bind(solicitacao: SolicitacaoCadastro, listener: SolicitacaoClickListener) {
            nomeSolicitante.text = solicitacao.nome
            val papelFormatado = solicitacao.papelSolicitado.replaceFirstChar { it.titlecase() }
            papelSolicitado.text = "Papel Solicitado: $papelFormatado"

            botaoAprovar.setOnClickListener {
                listener.onAprovarClick(solicitacao)
            }

            botaoRejeitar.setOnClickListener {
                listener.onRejeitarClick(solicitacao)
            }
        }
    }
}