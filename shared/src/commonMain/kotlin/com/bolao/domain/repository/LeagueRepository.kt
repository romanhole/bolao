package com.bolao.domain.repository

import com.bolao.domain.model.League
import kotlinx.coroutines.flow.Flow

interface LeagueRepository {
    fun getUserLeagues(): Flow<List<League>>
    suspend fun createLeague(name: String): Result<League>
    suspend fun joinLeague(inviteCode: String): Result<Unit>
}
