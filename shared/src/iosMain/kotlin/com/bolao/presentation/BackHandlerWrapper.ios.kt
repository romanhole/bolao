package com.bolao.presentation

import androidx.compose.runtime.Composable

/**
 * Implementação iOS do BackHandlerWrapper.
 * No iOS, o swipe-back é nativo — não precisamos interceptar nada.
 */
@Composable
actual fun BackHandlerWrapper(enabled: Boolean, onBack: () -> Unit) {
    // No-op: iOS usa gesto nativo de voltar
}
