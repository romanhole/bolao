package com.bolao.domain.model

/**
 * Representa a sessão do usuário autenticado.
 * Mapeado a partir do UserInfo do Supabase Auth SDK.
 */
data class UserSession(
    val userId: String,
    val email: String,
)
