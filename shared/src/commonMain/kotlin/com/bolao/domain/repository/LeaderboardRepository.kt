package com.bolao.domain.repository

import com.bolao.domain.model.LeaderboardItem

/**
 * Repositório para obter os dados do ranking de usuários.
 */
interface LeaderboardRepository {
    
    /**
     * Busca o ranking completo ordenado do primeiro para o último.
     * Retorna [Result.success] com a lista se bem-sucedido, ou [Result.failure] em caso de erro.
     */
    suspend fun getLeaderboard(leagueId: String): Result<List<LeaderboardItem>>
}
