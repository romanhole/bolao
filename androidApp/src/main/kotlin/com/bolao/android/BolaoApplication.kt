package com.bolao.android

import android.app.Application
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

        val authRepository: AuthRepository by inject()
        val pushTokenRepository: PushTokenRepository by inject()
        
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@addOnCompleteListener
            }

            val token = task.result
            CoroutineScope(Dispatchers.IO).launch {
                val user = authRepository.currentUser.firstOrNull()
                if (user != null) {
                    pushTokenRepository.saveToken(user.userId, token)
                }
            }
        }
    }
}
