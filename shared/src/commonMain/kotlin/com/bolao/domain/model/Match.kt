package com.bolao.domain.model

import kotlinx.datetime.Instant

/**
 * Entidade de domínio que representa uma partida de futebol.
 *
 * Esta é a entidade central do domínio. Todos os dados vêm exclusivamente
 * do nosso backend (Firestore / Supabase) — nunca de APIs externas.
 *
 * @property id           ID único da partida no nosso banco.
 * @property homeTeam     Time mandante.
 * @property awayTeam     Time visitante.
 * @property homeScore    Gols do mandante (null antes do início).
 * @property awayScore    Gols do visitante (null antes do início).
 * @property status       Estado atual do jogo (sealed class type-safe).
 * @property scheduledAt  Data/hora agendada para o início (UTC).
 * @property competition  Nome da competição (ex: "Copa do Mundo", "Brasileirão").
 * @property round        Rodada ou fase (ex: "Rodada 1", "Quartas de Final").
 */

data class Match(
    val id: String,
    val homeTeam: Team,
    val awayTeam: Team,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val homeScore90: Int? = null,
    val awayScore90: Int? = null,
    val homeScoreEt: Int? = null,
    val awayScoreEt: Int? = null,
    val penaltyWinner: String? = null,
    val isKnockout: Boolean = false,
    val homeOdd: Double? = null,
    val drawOdd: Double? = null,
    val awayOdd: Double? = null,
    val stageMultiplier: Float = 1.0f,
    val status: GameStatus = GameStatus.Scheduled,
    val scheduledAt: Instant,
    val competition: String,
    val round: String,
) {
    /**
     * Retorna true se a janela de palpites ainda está aberta.
     * Regra de negócio: palpites são aceitos apenas antes da partida começar, usando relógio local.
     */
    val isPredictionAllowed: Boolean
        get() = status is GameStatus.Scheduled && kotlinx.datetime.Clock.System.now() < scheduledAt

    /** Placar formatado para exibição. Se houver prorrogação, mostra o placar atual dela. */
    val scoreDisplay: String
        get() = when {
            homeScoreEt != null && awayScoreEt != null -> "$homeScoreEt – $awayScoreEt"
            homeScore != null && awayScore != null -> "$homeScore – $awayScore"
            else -> "– : –"
        }

    /** Time que de fato se classificou após um empate nos 90min (calculado no app para UI) */
    val actualQualifier: Team?
        get() {
            if (!isKnockout) return null
            if (homeScore90 == null || awayScore90 == null) return null
            if (homeScore90 != awayScore90) return null
            return when {
                penaltyWinner == "home" -> homeTeam
                penaltyWinner == "away" -> awayTeam
                homeScoreEt != null && awayScoreEt != null && homeScoreEt > awayScoreEt -> homeTeam
                homeScoreEt != null && awayScoreEt != null && awayScoreEt > homeScoreEt -> awayTeam
                homeScore != null && awayScore != null && homeScore > awayScore -> homeTeam
                homeScore != null && awayScore != null && awayScore > homeScore -> awayTeam
                else -> null
            }
        }
}
