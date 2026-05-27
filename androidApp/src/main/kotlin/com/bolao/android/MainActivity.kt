package com.bolao.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.bolao.presentation.App

/**
 * Única Activity do app Android.
 * Delega toda a UI para o [App] composable raiz (módulo shared),
 * que gerencia o roteamento entre Login e MatchList com base no estado de auth.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}
