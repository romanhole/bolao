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

    /** Alterna entre modo login e modo cadastro, limpando erros. */
    fun toggleMode() {
        _uiState.update { it.copy(isLoginMode = !it.isLoginMode, error = null) }
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

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = if (state.isLoginMode) {
                authRepository.login(state.email.trim(), state.password)
            } else {
                authRepository.signUp(state.email.trim(), state.password)
            }

            result
                .onSuccess {
                    // Sucesso: authRepository.authState emitirá Authenticated
                    // e App.kt navegará automaticamente para MatchListScreen
                    _uiState.update { it.copy(isLoading = false) }
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
        else                                                    -> "Erro ao autenticar. Tente novamente."
    }
}
