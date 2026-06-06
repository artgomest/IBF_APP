package com.ibf.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ibf.app.R
import com.ibf.app.data.models.SolicitacaoCadastro

class AprovacaoUsuarioAdapter(
    private val lista: List<SolicitacaoCadastro>,
    private val onAprovar: (SolicitacaoCadastro) -> Unit,
    private val onRejeitar: (SolicitacaoCadastro) -> Unit
) : RecyclerView.Adapter<AprovacaoUsuarioAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textNome: TextView = itemView.findViewById(R.id.text_nome)
        val textEmail: TextView = itemView.findViewById(R.id.text_email)
        val textPapel: TextView = itemView.findViewById(R.id.text_papel)
        val textRede: TextView = itemView.findViewById(R.id.text_rede)
        val btnAprovar: Button = itemView.findViewById(R.id.btn_aprovar)
        val btnRejeitar: Button = itemView.findViewById(R.id.btn_rejeitar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_aprovacao_usuario, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]
        holder.textNome.text = item.nome
        holder.textEmail.text = item.email
        holder.textPapel.text = "Papel: ${item.papelSolicitado.replaceFirstChar { it.uppercase() }}"
        holder.textRede.text = if (item.redeId.isNotEmpty()) "Rede: ${item.redeId}" else "Rede: Geral"
        
        holder.btnAprovar.setOnClickListener { onAprovar(item) }
        holder.btnRejeitar.setOnClickListener { onRejeitar(item) }
    }

    override fun getItemCount(): Int = lista.size
}
