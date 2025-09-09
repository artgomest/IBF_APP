package com.ibf.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ibf.app.R // Importação de R
import com.ibf.app.data.models.UsuarioRede // Importação do modelo UsuarioRede

// Data class para representar o usuário na lista (se estiver em um arquivo separado, apague daqui)
// Se UsuarioRede.kt está em data.models, esta data class não precisa estar aqui.
// data class UsuarioRede(...)

class UsuarioRedeAdapter(
    private val listaUsuarios: MutableList<UsuarioRede>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<UsuarioRedeAdapter.UsuarioViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(usuario: UsuarioRede)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_usuario_rede, parent, false)
        return UsuarioViewHolder(view)
    }

    override fun getItemCount() = listaUsuarios.size

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        val usuario = listaUsuarios[position]
        holder.nome.text = usuario.nome
        holder.dataNascimento.text = usuario.dataNascimento ?: "Não informada"

        holder.itemView.setOnClickListener {
            listener.onItemClick(usuario)
        }
    }

    fun atualizarLista(novaLista: List<UsuarioRede>) {
        listaUsuarios.clear()
        listaUsuarios.addAll(novaLista)
        notifyDataSetChanged() // Mantido intencionalmente
    }

    class UsuarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nome: TextView = itemView.findViewById(R.id.text_nome_usuario)
        val dataNascimento: TextView = itemView.findViewById(R.id.text_data_nascimento_usuario)
    }
}