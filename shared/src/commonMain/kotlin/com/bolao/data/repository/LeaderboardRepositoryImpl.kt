package com.bolao.data.repository

import com.bolao.data.remote.dto.LeaderboardDto
import com.bolao.domain.model.LeaderboardItem
import com.bolao.domain.repository.LeaderboardRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

/**
 * Implementação do [LeaderboardRepository] consumindo a view SQL `leaderboard` no Supabase via HTTP PostgREST.
 * Não utiliza Realtime, faz fetch sob demanda.
 */
class LeaderboardRepositoryImpl(
    private val supabase: SupabaseClient,
) : LeaderboardRepository {

    override suspend fun getLeaderboard(): Result<List<LeaderboardItem>> = runCatching {
        // Faz um select simples na view. A ordenação já é garantida pela lógica da view no backend.
        val dtos = supabase.postgrest["leaderboard"].select().decodeList<LeaderboardDto>()
        
        dtos.map { dto ->
            LeaderboardItem(
                userId               = dto.userId,
                nickname             = dto.nickname ?: dto.userId.take(8),
                totalPoints          = dto.totalPoints,
                totalPredictionsMade = dto.totalPredictionsMade,
                exactMatches         = dto.exactMatches,
            )
        }
    }
}
