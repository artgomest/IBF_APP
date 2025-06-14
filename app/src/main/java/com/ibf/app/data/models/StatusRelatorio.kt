package com.ibf.app.data.models

sealed class StatusRelatorio {
    // Guarda um relatório que foi de fato enviado
    data class Enviado(val relatorio: Relatorio) : StatusRelatorio()

    // Guarda as informações de um relatório que era esperado, mas não foi enviado
    data class Faltante(val dataEsperada: String, val nomeRede: String) : StatusRelatorio()
}