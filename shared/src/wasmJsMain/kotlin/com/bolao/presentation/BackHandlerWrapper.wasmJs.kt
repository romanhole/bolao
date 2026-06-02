package com.bolao.presentation

import androidx.compose.runtime.Composable

/**
 * No navegador, não gerenciamos o botão físico de voltar.
 * A própria navegação da UI ou as setas do navegador assumem o controle.
 */
@Composable
actual fun BackHandlerWrapper(enabled: Boolean, onBack: () -> Unit) {
    // No-op para WebAssembly
}
