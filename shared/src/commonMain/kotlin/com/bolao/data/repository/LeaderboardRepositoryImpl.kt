package com.bolao.data.repository

import com.bolao.data.remote.dto.LeaderboardDto
import com.bolao.domain.model.LeaderboardItem
import com.bolao.domain.repository.LeaderboardRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

/**
 * Implementação do [LeaderboardRepository] consumindo a view SQL `league_leaderboard`.
 */
class LeaderboardRepositoryImpl(
    private val supabase: SupabaseClient,
) : LeaderboardRepository {

    override suspend fun getLeaderboard(leagueId: String): Result<List<LeaderboardItem>> = runCatching {
        val dtos = supabase.postgrest["league_leaderboard"]
            .select { filter { eq("league_id", leagueId) } }
            .decodeList<LeaderboardDto>()
        
        dtos.map { dto ->
            LeaderboardItem(
                userId               = dto.userId,
                nickname             = dto.nickname,
                totalPoints          = dto.totalPoints,
                totalPredictionsMade = dto.totalPredictionsMade,
                exactMatches         = dto.exactMatches,
            )
        }
    }
}
