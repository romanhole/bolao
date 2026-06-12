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

    // Fluxo "Esqueci minha senha"
    val isForgotPasswordMode: Boolean    = false,
    val forgotPasswordEmail: String      = "",
    val forgotPasswordLoading: Boolean   = false,
    val forgotPasswordError: String?     = null,
    // Fluxo "Definir nova senha"
    val isOtpMode: Boolean               = false,
    val otpCode: String                  = "",
    val otpLoading: Boolean              = false,
    val otpError: String?                = null,

    val isNewPasswordMode: Boolean       = false,
    val newPassword: String              = "",
    val confirmNewPassword: String       = "",
    val isNewPasswordVisible: Boolean    = false,
    val isConfirmNewPasswordVisible: Boolean = false,
    val resetPasswordLoading: Boolean    = false,
    val resetPasswordError: String?      = null,
)

