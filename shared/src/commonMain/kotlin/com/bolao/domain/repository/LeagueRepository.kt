package com.bolao.domain.repository

import com.bolao.domain.model.League
import kotlinx.coroutines.flow.Flow

interface LeagueRepository {
    fun getUserLeagues(): Flow<List<League>>
    suspend fun createLeague(name: String, nickname: String): Result<League>
    suspend fun joinLeague(inviteCode: String, nickname: String): Result<Unit>
    suspend fun getLeagueById(leagueId: String): Result<League>
    suspend fun removeMember(leagueId: String, userId: String): Result<Unit>
    suspend fun renewInviteCode(leagueId: String): Result<String>
}
