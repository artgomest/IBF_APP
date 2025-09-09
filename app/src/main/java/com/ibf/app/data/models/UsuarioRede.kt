package com.ibf.app.data.models

data class UsuarioRede(
    val uid: String,
    val nome: String,
    val papel: String, // O papel real que ele tem na rede
    val statusAprovacao: String = "pendente", // NOVO CAMPO: "pendente", "aprovado", "rejeitado"
    val dataNascimento: String? = null
)