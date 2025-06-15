package com.ibf.app.data.models

data class Perfil(
    val rede: String,
    val papel: String,
    val statusAprovacao: String = "aprovado" // Assumimos que perfis já logados (Pastor/Líder/Secretário) são aprovados por padrão.
)