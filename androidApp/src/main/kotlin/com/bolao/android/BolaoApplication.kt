package com.bolao.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.bolao.di.networkModule
import com.bolao.di.repositoryModule
import com.bolao.di.viewModelModule
import com.bolao.domain.repository.AuthRepository
import com.bolao.domain.repository.PushTokenRepository
import com.bolao.platform.AppContext
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Application class do Android.
 * Inicializa o Koin com todos os módulos da aplicação.
 */
class BolaoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppContext.init(this)

        startKoin {
            androidContext(this@BolaoApplication)
            modules(
                networkModule,
                repositoryModule,
                viewModelModule,
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "bolao_alerts",
                "Alertas do Bolão",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val authRepository: AuthRepository by inject()
        val pushTokenRepository: PushTokenRepository by inject()
        val settingsManager: com.russhwolf.settings.Settings by inject()

        CoroutineScope(Dispatchers.IO).launch {
            authRepository.currentUser.collect { user ->
                if (user != null) {
                    // Só registra o token se as notificações estiverem ativadas nas configs
                    val isEnabled = settingsManager.getBoolean("notifications_enabled", true)
                    val hoursBefore = settingsManager.getInt("notification_hours_before", 1)

                    if (isEnabled) {
                        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    pushTokenRepository.saveToken(user.userId, task.result, hoursBefore)
                                }
                            }
                        }
                    }
                }
            }
        }

        com.bolao.presentation.settings.TokenSyncManager.syncAction = {
            CoroutineScope(Dispatchers.IO).launch {
                val user = authRepository.currentUser.firstOrNull()
                if (user != null) {
                    val isEnabled = settingsManager.getBoolean("notifications_enabled", true)
                    val hoursBefore = settingsManager.getInt("notification_hours_before", 1)
                    if (isEnabled) {
                        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    pushTokenRepository.saveToken(user.userId, task.result, hoursBefore)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
