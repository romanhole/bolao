package com.bolao.data.repository.fake

import com.bolao.domain.model.GameStatus
import com.bolao.domain.model.Match
import com.bolao.domain.model.Team
import com.bolao.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Implementação fake de [MatchRepository] para desenvolvimento e testes.
 *
 * Fornece partidas em diferentes estados para validar toda a lógica de UI:
 * - match_1: Agendada (palpite habilitado)
 * - match_2: Ao vivo (palpite desabilitado, placar visível)
 * - match_3: Encerrada (pontos exibidos)
 *
 * TODO: substituir por [FirebaseMatchRepository] ou [SupabaseMatchRepository]
 *       ao integrar o backend real.
 */
class FakeMatchRepository : MatchRepository {

    private val teams = mapOf(
        "bra" to Team("bra", "Brasil",     "BRA", ""),
        "arg" to Team("arg", "Argentina",  "ARG", ""),
        "fra" to Team("fra", "França",     "FRA", ""),
        "eng" to Team("eng", "Inglaterra", "ENG", ""),
        "ger" to Team("ger", "Alemanha",   "GER", ""),
        "esp" to Team("esp", "Espanha",    "ESP", ""),
    )

    private val now = Clock.System.now()

    private val sampleMatches = listOf(
        // Partida 1 — Agendada: palpite ainda aberto
        Match(
            id          = "match_1",
            homeTeam    = teams.getValue("bra"),
            awayTeam    = teams.getValue("arg"),
            status      = GameStatus.Scheduled,
            scheduledAt = now + 3.hours + 30.minutes,
            competition = "Copa do Mundo 2026",
            round       = "Fase de Grupos · Grupo C",
        ),
        // Partida 2 — Ao vivo: palpite bloqueado, placar em andamento
        Match(
            id          = "match_2",
            homeTeam    = teams.getValue("fra"),
            awayTeam    = teams.getValue("eng"),
            homeScore   = 2,
            awayScore   = 1,
            status      = GameStatus.Live(minutePlayed = 67),
            scheduledAt = now - 1.hours - 7.minutes,
            competition = "Copa do Mundo 2026",
            round       = "Fase de Grupos · Grupo D",
        ),
        // Partida 3 — Encerrada: pontos calculados pelo backend
        Match(
            id          = "match_3",
            homeTeam    = teams.getValue("ger"),
            awayTeam    = teams.getValue("esp"),
            homeScore   = 1,
            awayScore   = 3,
            status      = GameStatus.Finished,
            scheduledAt = now - 2.days - 4.hours,
            competition = "Copa do Mundo 2026",
            round       = "Fase de Grupos · Grupo E",
        ),
    )

    override fun observeMatchesByCompetition(competitionId: String): Flow<List<Match>> =
        flowOf(sampleMatches)

    override fun observeMatchById(matchId: String): Flow<Match?> =
        flowOf(sampleMatches.find { it.id == matchId })

    override fun observeMatchesByRound(competitionId: String, round: String): Flow<List<Match>> =
        flowOf(sampleMatches.filter { it.round == round })
}
