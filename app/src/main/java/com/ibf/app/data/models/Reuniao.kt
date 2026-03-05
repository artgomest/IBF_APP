package com.ibf.app.data.models

import com.google.firebase.Timestamp

data class Reuniao(
    var id: String = "",
    var liderUid: String = "",
    var membroUid: String = "",
    var membroNome: String = "",
    var dataHora: Timestamp? = null,
    var local: String = "",
    var observacoes: String = "",
    var status: String = "Agendada", // Agendada, Concluída, Cancelada
    var rede: String = "",
    var audioUrl: String = "",
    var fotoUrl: String = "",
    var resumo: String = "",
    var relatorioPreenchido: Boolean = false
)
