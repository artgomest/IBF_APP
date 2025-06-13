package com.ibf.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PerfilAdapter(
    private val listaPerfis: List<Perfil>,
    private val listener: OnPerfilClickListener
) : RecyclerView.Adapter<PerfilAdapter.PerfilViewHolder>() {

    interface OnPerfilClickListener {
        fun onPerfilClick(perfil: Perfil)
    }

    inner class PerfilViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val redeTextView: TextView = itemView.findViewById(R.id.item_perfil_rede)
        val papelTextView: TextView = itemView.findViewById(R.id.item_perfil_papel)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onPerfilClick(listaPerfis[position])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PerfilViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_perfil, parent, false)
        return PerfilViewHolder(view)
    }

    override fun getItemCount() = listaPerfis.size

    override fun onBindViewHolder(holder: PerfilViewHolder, position: Int) {
        val perfilAtual = listaPerfis[position]
        holder.redeTextView.text = perfilAtual.rede
        holder.papelTextView.text = perfilAtual.papel
    }
}