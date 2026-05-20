package com.bolao.android

import android.app.Application
import com.bolao.di.networkModule
import com.bolao.di.repositoryModule
import com.bolao.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Application class do Android.
 * Inicializa o Koin com todos os módulos da aplicação.
 */
class BolaoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@BolaoApplication)
            modules(
                networkModule,
                repositoryModule,
                viewModelModule,
            )
        }
    }
}
