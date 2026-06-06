package com.ibf.app.data.models

data class Usuario(
    val uid: String = "",
    val nome: String = "",
    val email: String = "",
    var funcoes: HashMap<String, String> = HashMap()
)
