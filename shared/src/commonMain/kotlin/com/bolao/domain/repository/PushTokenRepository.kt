package com.bolao.domain.repository

interface PushTokenRepository {
    /**
     * Salva o token FCM no banco de dados para o usuário logado.
     */
    suspend fun saveToken(userId: String, token: String): Result<Unit>
    
    /**
     * Remove o token FCM do banco de dados (por exemplo, no logout).
     */
    suspend fun deleteToken(token: String): Result<Unit>
}
