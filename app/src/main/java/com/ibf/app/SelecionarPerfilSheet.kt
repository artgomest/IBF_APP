package com.ibf.app

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.Serializable

class SelecionarPerfilSheet : BottomSheetDialogFragment(), PerfilAdapter.OnPerfilClickListener {

    // Interface para comunicar as ações de volta para a Activity
    private var listener: PerfilSelecionadoListener? = null

    interface PerfilSelecionadoListener {
        fun onPerfilSelecionado(rede: String, papel: String)
        fun onLogoutSelecionado() // Nova função para o logout
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? PerfilSelecionadoListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.sheet_selecionar_perfil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerViewPerfis = view.findViewById<RecyclerView>(R.id.recyclerViewPerfis)
        val sheetTitle = view.findViewById<TextView>(R.id.sheet_title)
        val logoutButton = view.findViewById<Button>(R.id.buttonSheetLogout)

        // Pega os dados que foram passados
        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        val funcoes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("FUNCOES", HashMap::class.java) as? HashMap<String, String>
        } else {
            arguments?.getSerializable("FUNCOES") as? HashMap<String, String>
        }
        val nomeUsuario = arguments?.getString("NOME_USUARIO") ?: ""

        sheetTitle.text = "$nomeUsuario, selecione o perfil:"

        val listaDePerfis = funcoes?.map { Perfil(it.key, it.value) } ?: emptyList()

        // Configura o RecyclerView
        recyclerViewPerfis.layoutManager = LinearLayoutManager(context)
        recyclerViewPerfis.adapter = PerfilAdapter(listaDePerfis, this)

        // Configura o clique do botão de logout
        logoutButton.setOnClickListener {
            listener?.onLogoutSelecionado()
            dismiss()
        }
    }

    // Chamado quando um perfil é clicado na lista
    override fun onPerfilClick(perfil: Perfil) {
        listener?.onPerfilSelecionado(perfil.rede, perfil.papel)
        dismiss()
    }

    companion object {
        fun newInstance(funcoes: HashMap<String, String>, nomeUsuario: String): SelecionarPerfilSheet {
            val args = Bundle()
            args.putSerializable("FUNCOES", funcoes as Serializable)
            args.putString("NOME_USUARIO", nomeUsuario)
            val fragment = SelecionarPerfilSheet()
            fragment.arguments = args
            return fragment
        }
    }
}