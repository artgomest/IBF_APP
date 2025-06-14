package com.ibf.app.ui.relatorios

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ibf.app.R
import android.widget.Toast// Importação de R

class SelecionarRelatorioSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_selecionar_relatorio, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Exemplo: Botões para "Ver Relatórios" e "Criar Novo Relatório"
        view.findViewById<View>(R.id.button_ver_relatorios).setOnClickListener {
            Toast.makeText(context, "Ver Relatórios clicado", Toast.LENGTH_SHORT).show()
            dismiss() // Fecha o bottom sheet
            // Implementar navegação para a tela de relatórios (ex: LiderStatusRelatoriosActivity)
        }

        view.findViewById<View>(R.id.button_criar_relatorio).setOnClickListener {
            Toast.makeText(context, "Criar Novo Relatório clicado", Toast.LENGTH_SHORT).show()
            dismiss()
            // Implementar navegação para a tela de formulário de relatório (ex: FormularioRedeActivity)
        }
    }
}