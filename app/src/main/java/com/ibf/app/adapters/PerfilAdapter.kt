package com.ibf.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.ibf.app.R // VERIFIQUE ESTA IMPORTAÇÃO!
import com.ibf.app.data.models.Perfil

class PerfilAdapter(
    private val listaPerfis: List<Perfil>,
    nomeUsuario: String,
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
        holder.perfilButton.text = context.getString(R.string.perfil_button_text, papelFormatado, perfil.rede)

        holder.itemView.setOnClickListener {
            listener.onItemClick(perfil)
        }
    }

    class PerfilViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // LINHA 42: VERIFIQUE ESTA REFERÊNCIA E SE item_perfil.xml TEM O ID item_perfil_button
        val perfilButton: Button = itemView.findViewById(R.id.card_perfil_button)
    }
}