package com.bolao.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolao.domain.repository.AuthRepository
import com.bolao.domain.repository.PushTokenRepository
import com.bolao.presentation.utils.NotificationPermissionHelper
import com.russhwolf.settings.Settings
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object TokenSyncManager {
    var syncAction: (() -> Unit)? = null
}

data class SettingsUiState(
    val isNotificationsEnabled: Boolean = true,
    val hoursBeforeMatch: Int = 1,
    val showPermissionDialog: Boolean = false,
    val isLoading: Boolean = false
)

class SettingsViewModel(
    private val pushTokenRepository: PushTokenRepository,
    private val authRepository: AuthRepository,
    private val settingsManager: Settings,
    private val permissionHelper: NotificationPermissionHelper,
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { 
            it.copy(
                isNotificationsEnabled = settingsManager.getBoolean("notifications_enabled", true),
                hoursBeforeMatch = settingsManager.getInt("notification_hours_before", 1)
            )
        }
    }

    fun isPushSupported(): Boolean = permissionHelper.isSupported()

    fun toggleNotifications(enabled: Boolean, token: String? = null) {
        if (enabled) {
            if (permissionHelper.hasPermission()) {
                // Permission granted, turn it on
                setNotificationsEnabled(true, token)
            } else {
                // Need permission
                _uiState.update { it.copy(showPermissionDialog = true) }
            }
        } else {
            // Turn off
            setNotificationsEnabled(false, token)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean, token: String?) {
        viewModelScope.launch {
            settingsManager.putBoolean("notifications_enabled", enabled)
            _uiState.update { it.copy(isNotificationsEnabled = enabled, showPermissionDialog = false) }

            val currentUser = supabase.auth.currentUserOrNull()?.id ?: return@launch

            if (enabled) {
                // Sincroniza o token nativamente via callback
                TokenSyncManager.syncAction?.invoke()
            } else {
                // Se desativou, limpa todos os tokens do banco imediatamente
                pushTokenRepository.deleteAllTokensForUser(currentUser)
            }
        }
    }

    fun updateHoursBeforeMatch(hours: Int, token: String?) {
        viewModelScope.launch {
            settingsManager.putInt("notification_hours_before", hours)
            _uiState.update { it.copy(hoursBeforeMatch = hours) }

            if (_uiState.value.isNotificationsEnabled) {
                // Se já estiver ativado, re-sincroniza o token com o novo horário via callback
                TokenSyncManager.syncAction?.invoke()
            }
        }
    }

    fun dismissPermissionDialog() {
        _uiState.update { it.copy(showPermissionDialog = false) }
    }

    fun openSystemSettings() {
        permissionHelper.openSystemSettings()
        dismissPermissionDialog()
    }
}
