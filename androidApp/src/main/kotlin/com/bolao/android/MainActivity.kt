package com.bolao.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.bolao.domain.repository.AuthRepository
import com.bolao.presentation.App
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Única Activity do app Android.
 * Delega toda a UI para o [App] composable raiz (módulo shared),
 * que gerencia o roteamento entre Login e MatchList com base no estado de auth.
 */
class MainActivity : ComponentActivity() {

    private val authRepository: AuthRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            App(appVersionCode = BuildConfig.VERSION_CODE)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.data?.toString()?.let { url ->
            lifecycleScope.launch {
                authRepository.handleDeepLink(url)
            }
        }
    }
}

