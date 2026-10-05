package com.bolao.testing

import com.bolao.domain.model.League
import com.bolao.domain.repository.LeagueRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

/**
 * Ligas em memória, com controles para o teste:
 * - [loadGate]: segura a primeira emissão até o teste completar o deferred, para que
 *   o estado de carregamento seja observável de forma determinística (sem sleep).
 * - [loadError]: faz o flow falhar, para testar o estado de erro.
 * - [createdLeagues] / [joinRequests]: registram o que a UI pediu.
 */
class FakeLeagueRepository(
    initialLeagues: List<League> = emptyList(),
    private val joinableLeagues: Map<String, League> = emptyMap(),
) : LeagueRepository {

    private val leagues = MutableStateFlow(initialLeagues)

    var loadGate: CompletableDeferred<Unit>? = null
    var loadError: Throwable? = null

    val createdLeagues = mutableListOf<Pair<String, String>>()
    val joinRequests = mutableListOf<Pair<String, String>>()

    override fun getUserLeagues(): Flow<List<League>> = flow {
        loadGate?.await()
        loadError?.let { throw it }
        emitAll(leagues)
    }

    override suspend fun createLeague(name: String, nickname: String): Result<League> {
        createdLeagues += name to nickname
        val league = League(
            id = "league_created_${createdLeagues.size}",
            name = name,
            inviteCode = "NEW${createdLeagues.size}",
            ownerId = TestData.currentUser.userId,
        )
        leagues.update { it + league }
        return Result.success(league)
    }

    override suspend fun joinLeague(inviteCode: String, nickname: String): Result<Unit> {
        joinRequests += inviteCode to nickname
        val league = joinableLeagues[inviteCode]
            ?: return Result.failure(IllegalArgumentException("Código de convite inválido"))
        leagues.update { it + league }
        return Result.success(Unit)
    }

    override suspend fun getLeagueById(leagueId: String): Result<League> =
        leagues.value.find { it.id == leagueId }
            ?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException("Liga $leagueId não encontrada"))

    override suspend fun removeMember(leagueId: String, userId: String): Result<Unit> = Result.success(Unit)

    override suspend fun renewInviteCode(leagueId: String): Result<String> = Result.success("RENOVADO")
}
