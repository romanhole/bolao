package com.bolao.presentation.auth

/**
 * Estado da tela de autenticação.
 *
 * @param email             Conteúdo do campo de e-mail
 * @param password          Conteúdo do campo de senha
 * @param nickname          Apelido do usuário (obrigatório apenas no cadastro)
 * @param isPasswordVisible Controla a visibilidade do texto da senha
 * @param isLoading         Exibe spinner e desabilita o botão durante a requisição
 * @param error             Mensagem de erro a ser exibida (null = sem erro)
 * @param isLoginMode       `true` = tela de login / `false` = tela de cadastro
 */
data class AuthUiState(
    val email: String              = "",
    val password: String           = "",
    val confirmPassword: String    = "",

    val isPasswordVisible: Boolean        = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean                = false,
    val error: String?                    = null,
    val isLoginMode: Boolean              = true,

    val isConfirmEmailDialogVisible: Boolean = false,
    val resendCooldownSeconds: Int           = 0,

    // Trocar Senha
    val newPassword: String                  = "",
    val confirmNewPassword: String           = "",
    val isNewPasswordVisible: Boolean        = false,
    val isConfirmNewPasswordVisible: Boolean = false,
    val changePasswordLoading: Boolean       = false,
    val changePasswordError: String?         = null,
    val changePasswordSuccess: Boolean       = false,
)
