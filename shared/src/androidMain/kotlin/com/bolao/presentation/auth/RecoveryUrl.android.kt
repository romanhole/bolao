package com.bolao.presentation.auth

actual fun isRecoveryUrl(): Boolean = false

actual fun getRedirectUrl(type: String): String = "bolao://$type"

