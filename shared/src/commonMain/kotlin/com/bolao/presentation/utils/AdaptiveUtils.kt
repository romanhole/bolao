package com.bolao.presentation.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.ExperimentalComposeUiApi

enum class WindowWidthClass { Compact, Medium, Expanded }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun rememberWindowWidthClass(): WindowWidthClass {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    
    // Calcula a largura da janela em dp
    val widthDp = with(density) { windowInfo.containerSize.width.toDp() }

    return remember(widthDp) {
        when {
            widthDp < 600.dp -> WindowWidthClass.Compact
            widthDp < 840.dp -> WindowWidthClass.Medium
            else -> WindowWidthClass.Expanded
        }
    }
}
