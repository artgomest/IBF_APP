package com.ibf.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ibf.app.R
import com.ibf.app.data.models.Usuario

class GerenciarUsuarioAdapter(
    private var usuarios: List<Usuario>,
    private val onUsuarioClick: (Usuario) -> Unit
) : RecyclerView.Adapter<GerenciarUsuarioAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textNome: TextView = view.findViewById(R.id.text_nome)
        val textEmail: TextView = view.findViewById(R.id.text_email)
        val textFuncoes: TextView = view.findViewById(R.id.text_funcoes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gerenciar_usuario, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val usuario = usuarios[position]
        holder.textNome.text = usuario.nome
        holder.textEmail.text = usuario.email

        if (usuario.funcoes.isEmpty()) {
            holder.textFuncoes.text = "Sem funções cadastradas"
            holder.textFuncoes.setTextColor(android.graphics.Color.RED)
        } else {
            val funcoesStr = usuario.funcoes.entries.joinToString(", ") { "${it.value} (${it.key})" }
            holder.textFuncoes.text = "Funções: $funcoesStr"
            holder.textFuncoes.setTextColor(android.graphics.Color.parseColor("#0052cc"))
        }

        holder.itemView.setOnClickListener {
            onUsuarioClick(usuario)
        }
    }

    override fun getItemCount() = usuarios.size

    fun atualizarLista(novaLista: List<Usuario>) {
        usuarios = novaLista
        notifyDataSetChanged()
    }
}
