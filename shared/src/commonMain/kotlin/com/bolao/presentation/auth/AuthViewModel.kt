package com.bolao.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolao.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel da tela de autenticação.
 *
 * ## Responsabilidades
 * - Gerencia o [AuthUiState] (campos, erros, loading)
 * - Delega login/signup ao [AuthRepository]
 * - Traduz erros técnicos do Supabase para mensagens amigáveis
 *
 * ## Navegação
 * A navegação entre Login e MatchList é gerenciada pelo [App] composable raiz,
 * que observa [AuthRepository.authState]. Este ViewModel não precisa lidar com navegação.
 */
class AuthViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.isResetPasswordMode.collect { isReset ->
                _uiState.update { it.copy(isResetPasswordMode = isReset) }
            }
        }
        if (isRecoveryUrl()) {
            enterResetPasswordMode()
        }
    }

    // ── Atualizações de campos ─────────────────────────────────────────────────

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, error = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }



    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onConfirmPasswordChange(password: String) {
        _uiState.update { it.copy(confirmPassword = password, error = null) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    /** Alterna entre modo login e modo cadastro, limpando erros. */
    fun toggleMode() {
        _uiState.update { it.copy(isLoginMode = !it.isLoginMode, error = null) }
    }

    /** Encerra a sessão do usuário atual. App.kt reagirá automaticamente via authState. */
    suspend fun logout() {
        authRepository.logout()
    }

    // ── Submissão ─────────────────────────────────────────────────────────────

    /**
     * Executa login ou cadastro dependendo do [AuthUiState.isLoginMode].
     * Valida campos localmente antes de chamar o backend.
     */
    fun submit() {
        val state = _uiState.value

        // Validação local antes de chamar o backend
        if (state.email.isBlank()) {
            _uiState.update { it.copy(error = "Informe o e-mail") }
            return
        }
        if (state.password.isBlank()) {
            _uiState.update { it.copy(error = "Informe a senha") }
            return
        }
        if (!state.isLoginMode && state.password.length < 6) {
            _uiState.update { it.copy(error = "A senha deve ter pelo menos 6 caracteres") }
            return
        }
        if (!state.isLoginMode && state.password != state.confirmPassword) {
            _uiState.update { it.copy(error = "As senhas não coincidem") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = if (state.isLoginMode) {
                authRepository.login(state.email.trim(), state.password)
            } else {
                authRepository.signUp(state.email.trim(), state.password)
            }

            result
                .onSuccess {
                    if (state.isLoginMode) {
                        // Sucesso no login: authRepository.authState emitirá Authenticated
                        _uiState.update { it.copy(isLoading = false) }
                    } else {
                        // Sucesso no cadastro: exibe diálogo de e-mail e inicia cronômetro
                        _uiState.update { it.copy(isLoading = false, isConfirmEmailDialogVisible = true) }
                        startResendCooldown()
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error     = parseAuthError(error.message),
                        )
                    }
                }
        }
    }

    // ── Resend Email & Dialog ──────────────────────────────────────────────────

    fun dismissConfirmDialog() {
        _uiState.update { it.copy(isConfirmEmailDialogVisible = false) }
    }

    fun resendEmail() {
        val state = _uiState.value
        if (state.resendCooldownSeconds > 0 || state.email.isBlank()) return

        viewModelScope.launch {
            authRepository.resendConfirmationEmail(state.email)
            startResendCooldown()
        }
    }

    private fun startResendCooldown() {
        viewModelScope.launch {
            _uiState.update { it.copy(resendCooldownSeconds = 30) }
            while (_uiState.value.resendCooldownSeconds > 0) {
                kotlinx.coroutines.delay(1000)
                _uiState.update { it.copy(resendCooldownSeconds = it.resendCooldownSeconds - 1) }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Traduz mensagens de erro técnicas do Supabase para textos amigáveis em português.
     */
    private fun parseAuthError(message: String?): String = when {
        message == null                                         -> "Erro desconhecido. Tente novamente."
        message.contains("Invalid login credentials")          -> "E-mail ou senha incorretos."
        message.contains("Email not confirmed")                 -> "Confirme seu e-mail antes de entrar."
        message.contains("User already registered")            -> "Este e-mail já está cadastrado."
        message.contains("Password should be at least")        -> "A senha deve ter pelo menos 6 caracteres."
        message.contains("Unable to validate email address")   -> "E-mail inválido."
        message.contains("rate limit")                         -> "Muitas tentativas. Aguarde um momento."
        else                                                    -> "Erro: $message"
    }

    // ── Fluxo Esqueci minha senha / Reset de senha ─────────────────────────────

    fun onForgotPasswordClick() {
        _uiState.update { 
            it.copy(
                isForgotPasswordMode = true,
                forgotPasswordEmail = "",
                resetEmailSent = false,
                forgotPasswordLoading = false,
                forgotPasswordError = null
            )
        }
    }

    fun onForgotPasswordEmailChange(value: String) {
        _uiState.update { it.copy(forgotPasswordEmail = value, forgotPasswordError = null) }
    }

    fun onBackFromForgotPassword() {
        _uiState.update { 
            it.copy(
                isForgotPasswordMode = false,
                forgotPasswordError = null
            )
        }
    }

    fun submitForgotPassword() {
        val email = _uiState.value.forgotPasswordEmail.trim()
        if (email.isBlank()) {
            _uiState.update { it.copy(forgotPasswordError = "Informe o e-mail") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(forgotPasswordLoading = true, forgotPasswordError = null) }
            authRepository.sendPasswordResetEmail(email)
                .onSuccess {
                    _uiState.update { it.copy(forgotPasswordLoading = false, resetEmailSent = true) }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            forgotPasswordLoading = false, 
                            forgotPasswordError = parseAuthError(error.message)
                        ) 
                    }
                }
        }
    }

    fun enterResetPasswordMode() {
        viewModelScope.launch {
            authRepository.handleDeepLink("")
        }
        _uiState.update {
            it.copy(
                isResetPasswordMode = true,
                newPassword = "",
                confirmNewPassword = "",
                resetPasswordError = null
            )
        }
    }

    fun onNewPasswordChange(value: String) {
        _uiState.update { it.copy(newPassword = value, resetPasswordError = null) }
    }

    fun onConfirmNewPasswordChange(value: String) {
        _uiState.update { it.copy(confirmNewPassword = value, resetPasswordError = null) }
    }

    fun toggleNewPasswordVisibility() {
        _uiState.update { it.copy(isNewPasswordVisible = !_uiState.value.isNewPasswordVisible) }
    }

    fun toggleConfirmNewPasswordVisibility() {
        _uiState.update { it.copy(isConfirmNewPasswordVisible = !_uiState.value.isConfirmNewPasswordVisible) }
    }

    fun cancelResetPassword() {
        viewModelScope.launch {
            authRepository.logout()
            authRepository.clearResetPasswordMode()
            _uiState.update { 
                it.copy(
                    isResetPasswordMode = false,
                    newPassword = "",
                    confirmNewPassword = "",
                    resetPasswordError = null
                ) 
            }
        }
    }

    fun submitNewPassword() {
        val state = _uiState.value
        if (state.newPassword.isBlank()) {
            _uiState.update { it.copy(resetPasswordError = "Informe a nova senha") }
            return
        }
        if (state.newPassword.length < 6) {
            _uiState.update { it.copy(resetPasswordError = "A senha deve ter pelo menos 6 caracteres") }
            return
        }
        if (state.newPassword != state.confirmNewPassword) {
            _uiState.update { it.copy(resetPasswordError = "As senhas não coincidem") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(resetPasswordLoading = true, resetPasswordError = null) }
            authRepository.updatePassword(state.newPassword)
                .onSuccess {
                    authRepository.clearResetPasswordMode()
                    _uiState.update { 
                        it.copy(
                            resetPasswordLoading = false,
                            isResetPasswordMode = false,
                            newPassword = "",
                            confirmNewPassword = ""
                        ) 
                    }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            resetPasswordLoading = false, 
                            resetPasswordError = parseAuthError(error.message)
                        ) 
                    }
                }
        }
    }
}

