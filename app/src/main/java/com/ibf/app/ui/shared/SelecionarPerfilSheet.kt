package com.ibf.app.ui.shared

import android.annotation.SuppressLint
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
import com.ibf.app.R
import com.ibf.app.adapters.PerfilAdapter
import com.ibf.app.data.models.Perfil

class SelecionarPerfilSheet : BottomSheetDialogFragment() {

    interface PerfilSelecionadoListener {
        fun onPerfilSelecionado(rede: String, papel: String)
        fun onLogoutSelecionado()
    }

    private var listener: PerfilSelecionadoListener? = null

    companion object {
        private const val ARG_FUNCOES = "funcoes"
        private const val ARG_NOME_USUARIO = "nome_usuario"

        fun newInstance(funcoes: HashMap<String, String>, nomeUsuario: String): SelecionarPerfilSheet {
            val fragment = SelecionarPerfilSheet()
            val args = Bundle()
            args.putSerializable(ARG_FUNCOES, funcoes)
            args.putString(ARG_NOME_USUARIO, nomeUsuario)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var recyclerViewPerfis: RecyclerView
    private lateinit var perfilAdapter: PerfilAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is PerfilSelecionadoListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement PerfilSelecionadoListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_selecionar_perfil, container, false)
    }

    @SuppressLint("StringFormatInvalid")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nomeUsuario = arguments?.getString(ARG_NOME_USUARIO) ?: getString(R.string.usuario_padrao)

        // --- CORREÇÃO DO WARNING DE DEPRECIAÇÃO ---
        @Suppress("UNCHECKED_CAST", "DEPRECATION") // Suprime os avisos de cast e depreciação
        val funcoes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            arguments?.getSerializable(ARG_FUNCOES, HashMap::class.java) as? HashMap<String, String>
        } else {
            arguments?.getSerializable(ARG_FUNCOES) as? HashMap<String, String>
        }
        // --- FIM DA CORREÇÃO ---


        val greetingTextView: TextView = view.findViewById(R.id.text_greeting_bottom_sheet)
        greetingTextView.text = getString(R.string.ola_nome_bottom_sheet_placeholder, nomeUsuario)


        recyclerViewPerfis = view.findViewById(R.id.recycler_view_perfis)
        recyclerViewPerfis.layoutManager = LinearLayoutManager(context)

        val listaDePerfis = funcoes?.map { (rede, papel) -> Perfil(rede, papel) } ?: emptyList()

        perfilAdapter = PerfilAdapter(listaDePerfis, nomeUsuario, object : PerfilAdapter.OnItemClickListener {
            override fun onItemClick(perfil: Perfil) {
                listener?.onPerfilSelecionado(perfil.rede, perfil.papel)
                dismiss()
            }
        })
        recyclerViewPerfis.adapter = perfilAdapter

        val logoutButton: Button = view.findViewById(R.id.button_logout_bottom_sheet)
        logoutButton.setOnClickListener {
            listener?.onLogoutSelecionado()
            dismiss()
        }
    }
}