package com.bolao.presentation.utils

expect class NotificationPermissionHelper() {
    fun hasPermission(): Boolean
    fun openSystemSettings()
    fun isSupported(): Boolean
}
