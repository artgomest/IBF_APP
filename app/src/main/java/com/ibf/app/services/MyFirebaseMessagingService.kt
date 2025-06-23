package com.ibf.app.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ibf.app.R

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "FirebaseMsgService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Notificação recebida de: ${remoteMessage.from}")

        remoteMessage.notification?.let {
            mostrarNotificacao(it.title, it.body)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Novo Token FCM: $token")
        FirebaseAuth.getInstance().currentUser?.uid?.let { userId ->
            salvarTokenNoFirestore(userId, token)
        }
    }

    private fun salvarTokenNoFirestore(userId: String, token: String) {
        val firestore = FirebaseFirestore.getInstance()
        val userDocument = firestore.collection("usuarios").document(userId)
        userDocument.set(mapOf("fcmToken" to token), SetOptions.merge())
            .addOnSuccessListener { Log.d(TAG, "Token FCM salvo para o usuário $userId") }
            .addOnFailureListener { e -> Log.w(TAG, "Erro ao salvar token FCM para o usuário $userId", e) }
    }

    private fun mostrarNotificacao(titulo: String?, corpo: String?) {
        val channelId = "canal_padrao_ibf"
        val channelName = "Notificações Gerais"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // CORREÇÃO 1: Aumentamos a importância do canal
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logobranca)
            .setContentTitle(titulo)
            .setContentText(corpo)
            // CORREÇÃO 2: Aumentamos a prioridade da notificação
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
        }
    }
}