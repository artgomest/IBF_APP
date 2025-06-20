package com.ibf.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "FirebaseMsgService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "De: ${remoteMessage.from}")

        remoteMessage.notification?.let {
            Log.d(TAG, "Corpo da Notificação: ${it.body}")
            mostrarNotificacao(it.title, it.body)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Novo Token de Registro FCM: $token")
        // Futuramente, podemos salvar este token no perfil do usuário no Firestore
    }

    private fun mostrarNotificacao(titulo: String?, corpo: String?) {
        val channelId = "canal_padrao_ibf"
        val channelName = "Notificações Gerais"

        // Cria o Canal de Notificação (Obrigatório para Android 8.0 Oreo e superior)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_home) // Use um ícone seu. ic_home é uma boa opção.
            .setContentTitle(titulo)
            .setContentText(corpo)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        // Verifica se o app tem permissão para postar notificações antes de tentar.
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
        } else {
            Log.w(TAG, "Permissão para postar notificações não foi concedida.")
        }
    }
}