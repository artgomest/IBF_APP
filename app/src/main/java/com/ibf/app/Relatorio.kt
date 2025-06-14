// Exemplo: app/src/main/java/com/ibf/app/Relatorio.kt
package com.ibf.app

// Se o seu campo no Firestore for 'dataReuniao', e você espera uma String
// e 'data' também for uma string, certifique-se dos nomes e tipos.
data class Relatorio(
    var id: String = "", // 'var' é importante para 'apply { id = doc.id }'
    @JvmField val idRede: String = "",
    @JvmField val totalPessoas: Int = 0,
    @JvmField val totalVisitantes: Int = 0,
    @JvmField val valorOferta: Double = 0.0,
    @JvmField val data: String = "",
    @JvmField val dataReuniao: String = "",
    @JvmField val autorNome: String = ""
    // Adicione aqui todos os outros campos que o Firestore retorna para 'relatorios'
)