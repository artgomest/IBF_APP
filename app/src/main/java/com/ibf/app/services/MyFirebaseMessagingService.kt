package com.ibf.app.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ibf.app.R
import com.ibf.app.ui.main.MainActivity

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "FirebaseMsgService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Notificação recebida de: ${remoteMessage.from}")

        var titulo = remoteMessage.notification?.title
        var corpo = remoteMessage.notification?.body

        if (titulo == null && remoteMessage.data.isNotEmpty()) {
            titulo = remoteMessage.data["title"]
            corpo = remoteMessage.data["body"]
        }

        if (titulo != null || corpo != null) {
            mostrarNotificacao(titulo, corpo, remoteMessage.data)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Novo Token de Registro FCM: $token")
        FirebaseAuth.getInstance().currentUser?.uid?.let { userId ->
            salvarTokenNoFirestore(userId, token)
        }
    }

    private fun salvarTokenNoFirestore(userId: String, token: String) {
        val firestore = FirebaseFirestore.getInstance()
        val userDocument = firestore.collection("usuarios").document(userId)
        userDocument.update("fcmToken", token)
            .addOnSuccessListener { Log.d(TAG, "Token FCM salvo com sucesso para o usuário $userId") }
            .addOnFailureListener { e -> Log.w(TAG, "Erro ao salvar token FCM para o usuário $userId", e) }
    }

    private fun mostrarNotificacao(titulo: String?, corpo: String?, data: Map<String, String>) {
        val channelId = "canal_padrao_ibf_v2"
        val channelName = "Notificações Gerais"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            for ((key, value) in data) {
                putExtra(key, value)
            }
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            flags
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_home) // Substitua por um ícone seu
            .setContentTitle(titulo)
            .setContentText(corpo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
        }
    }
}