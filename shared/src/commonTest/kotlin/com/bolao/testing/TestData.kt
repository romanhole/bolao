package com.bolao.testing

import com.bolao.domain.model.GameStatus
import com.bolao.domain.model.LeaderboardItem
import com.bolao.domain.model.League
import com.bolao.domain.model.Match
import com.bolao.domain.model.Team
import com.bolao.domain.model.UserSession
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days

/**
 * Dados de teste fixos e conhecidos, compartilhados pelos testes de UI.
 *
 * Times sem `apiTeamId`: a UI usa o placeholder do escudo e nunca tenta
 * baixar imagem pela rede.
 */
object TestData {

    val currentUser = UserSession(userId = "user_ana", email = "ana@teste.dev")

    val leagueAmigos = League(id = "league_amigos", name = "Liga dos Amigos", inviteCode = "AMIGOS1", ownerId = "user_ana")
    val leagueCopa = League(id = "league_copa", name = "Bolão da Firma", inviteCode = "COPA26", ownerId = "user_bruno")

    val brasil = Team(id = "bra", name = "Brasil", shortName = "BRA", logoUrl = "", apiTeamId = null)
    val argentina = Team(id = "arg", name = "Argentina", shortName = "ARG", logoUrl = "", apiTeamId = null)

    /**
     * Partida agendada daqui a 2 dias. `Match.isPredictionAllowed` compara com o
     * relógio real, então a data é relativa a "agora" para o palpite estar sempre aberto.
     */
    fun scheduledMatch(id: String = "match_bra_arg") = Match(
        id = id,
        homeTeam = brasil,
        awayTeam = argentina,
        status = GameStatus.Scheduled,
        scheduledAt = Clock.System.now() + 2.days,
        competition = "Copa do Mundo 2026",
        round = "Rodada 1",
    )

    /** Ranking propositalmente fora de ordem: a tela é quem deve ordenar. */
    val unorderedLeaderboard = listOf(
        LeaderboardItem(userId = "user_ana", nickname = "Ana", totalPoints = 10, totalPredictionsMade = 5, exactMatches = 1),
        LeaderboardItem(userId = "user_bruno", nickname = "Bruno", totalPoints = 15, totalPredictionsMade = 5, exactMatches = 2),
        LeaderboardItem(userId = "user_carla", nickname = "Carla", totalPoints = 15, totalPredictionsMade = 5, exactMatches = 4),
    )
}
