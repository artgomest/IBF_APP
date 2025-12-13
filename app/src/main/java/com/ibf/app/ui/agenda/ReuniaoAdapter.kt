package com.ibf.app.ui.agenda

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ibf.app.R
import com.ibf.app.data.models.Reuniao
import java.text.SimpleDateFormat
import java.util.Locale

class ReuniaoAdapter(
    private var reunioes: List<Reuniao>
) : RecyclerView.Adapter<ReuniaoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textNome: TextView = view.findViewById(R.id.text_membro_nome)
        val textStatus: TextView = view.findViewById(R.id.text_status)
        val textData: TextView = view.findViewById(R.id.text_data_hora)
        val textLocal: TextView = view.findViewById(R.id.text_local)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reuniao, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val reuniao = reunioes[position]
        
        holder.textNome.text = reuniao.membroNome
        holder.textStatus.text = reuniao.status
        holder.textLocal.text = reuniao.local

        val format = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))
        holder.textData.text = reuniao.dataHora?.toDate()?.let { format.format(it) } ?: "Data N/A"

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, DetalhesReuniaoActivity::class.java)
            intent.putExtra("REUNIAO_ID", reuniao.id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = reunioes.size

    fun updateList(newList: List<Reuniao>) {
        reunioes = newList
        notifyDataSetChanged()
    }
}
