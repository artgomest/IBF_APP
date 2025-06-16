package com.ibf.app.data.models

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class SolicitacaoCadastro(
    @JvmField var id: String = "", // O ID do documento no Firestore
    @JvmField val uid: String = "", // O UID do usuário criado no Firebase Auth
    @JvmField val nome: String = "",
    @JvmField val email: String = "",
    @JvmField val redeId: String = "", // A rede para a qual o usuário está sendo solicitado
    @JvmField val papelSolicitado: String = "", // O papel que o líder selecionou (ex: "secretario")
    @JvmField var status: String = "pendente", // Status: "pendente", "aprovado", "rejeitado"
    @JvmField val solicitadoPorUid: String = "", // UID de quem fez o cadastro (o líder/pastor)
    @ServerTimestamp @JvmField val timestamp: Date? = null
)