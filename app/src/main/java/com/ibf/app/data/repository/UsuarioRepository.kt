package com.ibf.app.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class UsuarioRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val collectionRef = firestore.collection("usuarios")

    fun salvarNovoUsuario(uid: String, dados: Map<String, Any>): Task<Void> {
        return collectionRef.document(uid).set(dados)
    }

    fun buscarUsuarioPorEmail(email: String): Task<QuerySnapshot> {
        return collectionRef.whereEqualTo("email", email).limit(1).get()
    }

    fun adicionarFuncao(uid: String, novaRede: String, novoPapel: String): Task<Void> {
        val updates = hashMapOf<String, Any>(
            "funcoes.$novaRede" to novoPapel,
            "redes" to FieldValue.arrayUnion(novaRede)
        )
        return collectionRef.document(uid).update(updates)
    }
    
    fun buscarUsuario(uid: String): Task<com.google.firebase.firestore.DocumentSnapshot> {
        return collectionRef.document(uid).get()
    }
}
