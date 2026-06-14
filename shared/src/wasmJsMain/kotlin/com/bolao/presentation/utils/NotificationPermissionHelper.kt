package com.bolao.presentation.utils

actual class NotificationPermissionHelper actual constructor() {
    actual fun hasPermission(): Boolean = false
    actual fun openSystemSettings() {}
    actual fun isSupported(): Boolean = false
}
