package com.bolao.presentation.auth

import kotlinx.browser.window

actual fun isRecoveryUrl(): Boolean {
    val hash = window.location.hash
    val href = window.location.href
    return hash.contains("type=recovery") || href.contains("type=recovery")
}
