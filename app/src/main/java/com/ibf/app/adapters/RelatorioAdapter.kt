package com.ibf.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.ibf.app.R // Importação de R
import com.ibf.app.data.models.Relatorio // Importação do modelo Relatorio
import com.ibf.app.data.models.StatusRelatorio // Importação do modelo StatusRelatorio

class RelatorioAdapter(
    private val listaStatus: List<StatusRelatorio>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(status: StatusRelatorio)
    }

    class EnviadoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.card_item_relatorio)
        val data: TextView = itemView.findViewById(R.id.item_data)
        val rede: TextView = itemView.findViewById(R.id.item_rede)
        val autor: TextView = itemView.findViewById(R.id.item_autor)
    }

    class FaltanteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.card_item_relatorio_faltante)
        val data: TextView = itemView.findViewById(R.id.item_faltante_data)
        val rede: TextView = itemView.findViewById(R.id.item_faltante_rede)
    }

    override fun getItemViewType(position: Int): Int {
        return when (listaStatus[position]) {
            is StatusRelatorio.Enviado -> TIPO_ENVIADO
            is StatusRelatorio.Faltante -> TIPO_FALTANTE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TIPO_ENVIADO) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_relatorio, parent, false)
            EnviadoViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_relatorio_faltante, parent, false)
            FaltanteViewHolder(view)
        }
    }

    override fun getItemCount() = listaStatus.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val statusAtual = listaStatus[position]
        val context = holder.itemView.context

        holder.itemView.setOnClickListener {
            listener.onItemClick(statusAtual)
        }

        if (holder.itemViewType == TIPO_ENVIADO && statusAtual is StatusRelatorio.Enviado) {
            val enviadoHolder = holder as EnviadoViewHolder
            val relatorio = statusAtual.relatorio
            enviadoHolder.data.text = context.getString(R.string.item_data_label, relatorio.dataReuniao)
            enviadoHolder.rede.text = context.getString(R.string.item_rede_label, relatorio.idRede)
            enviadoHolder.autor.text = context.getString(R.string.item_autor_label, relatorio.autorNome)

            enviadoHolder.cardView.setCardBackgroundColor(context.resources.getColor(R.color.status_green_dark_theme, null))
            enviadoHolder.cardView.strokeColor = context.resources.getColor(R.color.status_green, null)
            enviadoHolder.data.setTextColor(context.resources.getColor(R.color.white, null))
            enviadoHolder.rede.setTextColor(context.resources.getColor(R.color.white, null))
            enviadoHolder.autor.setTextColor(context.resources.getColor(R.color.white, null))

        } else if (holder.itemViewType == TIPO_FALTANTE && statusAtual is StatusRelatorio.Faltante) {
            val faltanteHolder = holder as FaltanteViewHolder
            faltanteHolder.data.text = context.getString(R.string.item_faltante_data_label, statusAtual.dataEsperada)
            faltanteHolder.rede.text = context.getString(R.string.item_faltante_rede_label, statusAtual.nomeRede)

            faltanteHolder.cardView.setCardBackgroundColor(context.resources.getColor(R.color.status_red_dark_theme, null))
            faltanteHolder.cardView.strokeColor = context.resources.getColor(R.color.status_red, null)
            faltanteHolder.data.setTextColor(context.resources.getColor(R.color.white, null))
            faltanteHolder.rede.setTextColor(context.resources.getColor(R.color.white, null))
        }
    }

    companion object {
        private const val TIPO_ENVIADO = 1
        private const val TIPO_FALTANTE = 2
    }
}