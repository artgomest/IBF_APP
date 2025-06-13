package com.ibf.app

import com.google.firebase.firestore.Exclude
import com.google.firebase.Timestamp

data class Relatorio(
    @get:Exclude var id: String = "", // O ID do documento no Firestore
    val dataReuniao: String = "",
    val idRede: String = "",
    val autorNome: String = "",
    val data: Timestamp = Timestamp.now(),
    val totalPessoas: Int = 0,
    val numeroVisitantes: Int = 0,
    val totalOfertas: Double = 0.0 // Usando Double para valores monetários
) {
    constructor() : this("", "", "", "")
}