package com.ibf.app.data.models

sealed class StatusRelatorio {
    data class Enviado(val relatorio: Relatorio) : StatusRelatorio()
    data class Faltante(val dataEsperada: String, val nomeRede: String) : StatusRelatorio()
}