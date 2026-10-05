package com.bolao.testing

import com.bolao.domain.model.Match
import com.bolao.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Partidas em memória; [matches] pode ser alterado pelo teste para simular o Realtime. */
class FakeMatchRepository(initialMatches: List<Match> = emptyList()) : MatchRepository {

    val matches = MutableStateFlow(initialMatches)

    override fun observeMatchesByCompetition(competitionId: String): Flow<List<Match>> = matches

    override fun observeMatchById(matchId: String): Flow<Match?> =
        matches.map { list -> list.find { it.id == matchId } }

    override fun observeMatchesByRound(competitionId: String, round: String): Flow<List<Match>> =
        matches.map { list -> list.filter { it.round == round } }
}
