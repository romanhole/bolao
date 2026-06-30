package com.bolao.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bolao.domain.repository.AuthRepository
import com.bolao.domain.repository.PushTokenRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class BolaoFirebaseMessagingService : FirebaseMessagingService() {

    private val authRepository: AuthRepository by inject()
    private val pushTokenRepository: PushTokenRepository by inject()
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    /**
     * Chamado se o FCM token for atualizado. Isso pode ocorrer se a segurança
     * do dispositivo for comprometida ou na primeira vez que o app for iniciado.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            val user = authRepository.currentUser.firstOrNull()
            if (user != null) {
                pushTokenRepository.saveToken(user.userId, token)
            }
        }
    }

    /**
     * Chamado quando a notificação é recebida enquanto o app está em FOREGROUND.
     * Se o app estiver em BACKGROUND, o Android cuida de mostrar a notificação automaticamente
     * (desde que a payload contenha 'notification').
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let {
            sendNotification(it.title ?: "Bolão Campeão", it.body ?: "")
        }
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )

        val channelId = "bolao_alerts"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // CUIDADO: Estamos usando android.R.drawable.ic_dialog_info apenas como placeholder para não quebrar build
        // O ideal é usar o ícone mipmap do app: R.mipmap.ic_launcher, porém precisamos expor o recurso no Android
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alertas do Bolão",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }
}
