package com.ibf.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView

class SelecionarRelatorioSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla (desenha) o nosso novo layout de bandeja
        return inflater.inflate(R.layout.sheet_selecionar_relatorio, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Encontra os componentes dentro da bandeja
        val cardRede = view.findViewById<MaterialCardView>(R.id.card_relatorio_rede)
        val cardFinanceiro = view.findViewById<MaterialCardView>(R.id.card_relatorio_financeiro)
        val closeButton = view.findViewById<ImageButton>(R.id.buttonCloseSheet)

        // Lógica para fechar a bandeja
        closeButton.setOnClickListener {
            dismiss() // Comando para fechar o BottomSheet
        }

        // Abre o formulário de rede que já temos
        cardRede.setOnClickListener {
            startActivity(Intent(requireContext(), FormularioRedeActivity::class.java))
            dismiss() // Fecha a bandeja após a seleção
        }

        // Apenas mostra uma mensagem, pois ainda não criamos este formulário
        cardFinanceiro.setOnClickListener {
            Toast.makeText(context, "Função a ser implementada!", Toast.LENGTH_SHORT).show()
        }
    }
}