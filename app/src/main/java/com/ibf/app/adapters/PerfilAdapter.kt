package com.ibf.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.ibf.app.R
import com.ibf.app.data.models.Perfil

class PerfilAdapter(
    private val listaPerfis: List<Perfil>,
    private val nomeUsuarioLogado: String, // Adicionado este parâmetro no construtor
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<PerfilAdapter.PerfilViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(perfil: Perfil)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PerfilViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_perfil, parent, false)
        return PerfilViewHolder(view)
    }

    override fun getItemCount() = listaPerfis.size

    override fun onBindViewHolder(holder: PerfilViewHolder, position: Int) {
        val perfil = listaPerfis[position]
        val context = holder.itemView.context

        val papelFormatado = perfil.papel.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        // Define o texto principal do perfil (ex: "Líder: Rede Alpha")
        holder.textPerfilPapelRede.text = context.getString(R.string.perfil_button_text, papelFormatado, perfil.rede)
        // Define o nome do usuário (ex: "Arthur Esteves")
        holder.textPerfilNomeUsuario.text = nomeUsuarioLogado // Usa o nome passado para o adapter

        // --- CORREÇÃO AQUI: Definir o clique no MaterialCardView ---
        holder.cardPerfilButton.setOnClickListener {
            listener.onItemClick(perfil)
        }
    }

    class PerfilViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // --- CORREÇÃO AQUI: Esperar um MaterialCardView ---
        val cardPerfilButton: MaterialCardView = itemView.findViewById(R.id.card_perfil_button) // <<--- O ID AGORA É card_perfil_button E O TIPO É MaterialCardView
        val textPerfilPapelRede: TextView = itemView.findViewById(R.id.text_perfil_papel_rede)
        val textPerfilNomeUsuario: TextView = itemView.findViewById(R.id.text_perfil_nome_usuario)
    }
}