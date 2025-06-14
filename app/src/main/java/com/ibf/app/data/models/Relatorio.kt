package com.ibf.app.data.models

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Relatorio(
    @JvmField var id: String = "",
    @JvmField val autorUid: String = "",
    @JvmField val autorNome: String = "",
    @JvmField val idRede: String = "",
    @JvmField val dataReuniao: String = "",
    @JvmField val comentarios: String = "",
    @JvmField val descricao: String = "",
    @JvmField val totalPessoas: Int = 0,
    @JvmField val totalVisitantes: Int = 0,
    @JvmField val valorOferta: Double = 0.0,
    @ServerTimestamp @JvmField val timestamp: Date? = null
)