package com.ibf.app.ui.shared

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ibf.app.R // Importação de R

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nomeUsuario = arguments?.getString(ARG_NOME_USUARIO) ?: "Usuário"
        @Suppress("UNCHECKED_CAST")
        val funcoes = arguments?.getSerializable(ARG_FUNCOES) as? HashMap<String, String>

        val greetingTextView: TextView = view.findViewById(R.id.text_greeting_bottom_sheet)
        greetingTextView.text = getString(R.string.ola_nome_bottom_sheet, nomeUsuario)

        val profilesContainer: LinearLayout = view.findViewById(R.id.layout_perfis_container)

        funcoes?.forEach { (rede, papel) ->
            val profileButton = LayoutInflater.from(context).inflate(R.layout.item_perfil_button, profilesContainer, false) as Button
            profileButton.text = getString(R.string.perfil_button_text, papel.capitalize(), rede) // Capitalize para ficar bonito
            profileButton.setOnClickListener {
                listener?.onPerfilSelecionado(rede, papel)
                dismiss()
            }
            profilesContainer.addView(profileButton)
        }

        val logoutButton: Button = view.findViewById(R.id.button_logout_bottom_sheet)
        logoutButton.setOnClickListener {
            listener?.onLogoutSelecionado()
            dismiss()
        }
    }
}