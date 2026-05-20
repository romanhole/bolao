package com.bolao.presentation.auth

/**
 * Estado da tela de autenticação.
 *
 * @param email             Conteúdo do campo de e-mail
 * @param password          Conteúdo do campo de senha
 * @param isPasswordVisible Controla a visibilidade do texto da senha
 * @param isLoading         Exibe spinner e desabilita o botão durante a requisição
 * @param error             Mensagem de erro a ser exibida (null = sem erro)
 * @param isLoginMode       `true` = tela de login / `false` = tela de cadastro
 */
data class AuthUiState(
    val email: String              = "",
    val password: String           = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean         = false,
    val error: String?             = null,
    val isLoginMode: Boolean       = true,
)
