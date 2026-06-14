package com.bolao.domain.repository

interface PushTokenRepository {
    /**
     * Salva o token FCM no banco de dados para o usuário logado.
     */
    suspend fun saveToken(userId: String, token: String, hoursBeforeMatch: Int = 1): Result<Unit>
    
    /**
     * Remove o token FCM do banco de dados (por exemplo, no logout).
     */
    suspend fun deleteToken(token: String): Result<Unit>
    
    /**
     * Remove todos os tokens de um usuário específico.
     */
    suspend fun deleteAllTokensForUser(userId: String): Result<Unit>
}
