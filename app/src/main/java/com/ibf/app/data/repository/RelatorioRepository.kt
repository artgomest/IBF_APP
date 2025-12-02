package com.ibf.app.data.repository

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.tasks.Task
import com.ibf.app.data.models.Relatorio

class RelatorioRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val collectionRef = firestore.collection("relatorios")

    fun salvarRelatorio(relatorioId: String?, dados: Map<String, Any>): Task<*> {
        val dadosComTimestamp = dados.toMutableMap().apply {
            put("timestamp", FieldValue.serverTimestamp())
        }

        return if (relatorioId != null) {
            collectionRef.document(relatorioId).update(dadosComTimestamp)
        } else {
            collectionRef.add(dadosComTimestamp)
        }
    }

    fun carregarRelatorio(id: String): Task<com.google.firebase.firestore.DocumentSnapshot> {
        return collectionRef.document(id).get()
    }
}
