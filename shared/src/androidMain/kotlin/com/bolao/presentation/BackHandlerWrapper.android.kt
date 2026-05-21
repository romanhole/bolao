package com.bolao.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/**
 * Implementação Android do BackHandlerWrapper.
 * Intercepta o botão físico de Voltar quando [enabled] = true.
 */
@Composable
actual fun BackHandlerWrapper(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
