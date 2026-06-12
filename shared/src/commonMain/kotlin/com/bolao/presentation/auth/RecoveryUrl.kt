package com.bolao.presentation.auth

expect fun isRecoveryUrl(): Boolean

expect fun getRedirectUrl(type: String): String

