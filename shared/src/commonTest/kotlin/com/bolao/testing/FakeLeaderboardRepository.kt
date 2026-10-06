package com.bolao.testing

import com.bolao.domain.model.LeaderboardItem
import com.bolao.domain.repository.LeaderboardRepository

/** Ranking fixo por liga; liga desconhecida devolve lista vazia. */
class FakeLeaderboardRepository(
    private val rankingByLeague: Map<String, List<LeaderboardItem>> = emptyMap(),
) : LeaderboardRepository {

    override suspend fun getLeaderboard(leagueId: String): Result<List<LeaderboardItem>> =
        Result.success(rankingByLeague[leagueId].orEmpty())
}
