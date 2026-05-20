package com.bolao.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bolao.presentation.matchlist.MatchListScreen

/**
 * Única Activity do app Android.
 * Delega toda a UI para o Compose Multiplatform (módulo shared).
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MatchListScreen()
        }
    }
}
