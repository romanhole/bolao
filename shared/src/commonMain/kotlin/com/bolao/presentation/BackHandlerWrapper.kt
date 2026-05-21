package com.bolao.presentation

import androidx.compose.runtime.Composable

/**
 * Wrapper KMP para o BackHandler do Android.
 * No iOS, o gesto de "swipe back" é nativo — não precisamos fazer nada.
 */
@Composable
expect fun BackHandlerWrapper(enabled: Boolean = true, onBack: () -> Unit)
