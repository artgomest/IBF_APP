package com.ibf.app.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ibf.app.R

// Data class para representar o usuário na lista
// Incluímos 'idRede' porque o campo 'funcoes' é um mapa, e precisamos
// saber o papel específico para a rede que estamos visualizando.
data class UsuarioRede(
    val uid: String,
    val nome: String,
    val papel: String
)

class UsuarioRedeAdapter(
    private val listaUsuarios: MutableList<UsuarioRede>,
    private val listener: OnItemClickListener // Se quiser cliques nos usuários
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
        // --- CORREÇÃO AQUI para usar string resource com placeholder ---
        val papelFormatado = usuario.papel.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } // Capitaliza a primeira letra
        holder.papel.text = holder.itemView.context.getString(R.string.papel_usuario_label_format, papelFormatado)

        holder.itemView.setOnClickListener {
            listener.onItemClick(usuario)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun atualizarLista(novaLista: List<UsuarioRede>) {
        listaUsuarios.clear()
        listaUsuarios.addAll(novaLista)
        notifyDataSetChanged() // Mantido intencionalmente para atualização completa da lista
    }

    class UsuarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nome: TextView = itemView.findViewById(R.id.text_nome_usuario)
        val papel: TextView = itemView.findViewById(R.id.text_papel_usuario)
    }
}