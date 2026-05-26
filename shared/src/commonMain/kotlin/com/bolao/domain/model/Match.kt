package com.bolao.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

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
@Serializable
data class Match(
    val id: String,
    val homeTeam: Team,
    val awayTeam: Team,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val homeOdd: Double? = null,
    val drawOdd: Double? = null,
    val awayOdd: Double? = null,
    val stageMultiplier: Int = 1,
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

    /** Placar formatado para exibição, ex: "2 – 1" ou "– : –" */
    val scoreDisplay: String
        get() = if (homeScore != null && awayScore != null) {
            "$homeScore – $awayScore"
        } else {
            "– : –"
        }
}
