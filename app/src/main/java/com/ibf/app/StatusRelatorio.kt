package com.ibf.app

// Esta "sealed class" funciona como um container que pode ter diferentes formas.
// Um item na nossa lista será ou um "Enviado" ou um "Faltante".
sealed class StatusRelatorio {
    // Guarda um relatório que foi de fato enviado
    data class Enviado(val relatorio: Relatorio) : StatusRelatorio()

    // Guarda as informações de um relatório que era esperado, mas não foi enviado
    data class Faltante(val dataEsperada: String, val nomeRede: String) : StatusRelatorio()
}