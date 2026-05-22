package com.bolao.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO para a tabela `matches` do Supabase com join embutido de times.
 *
 * ## Query PostgREST que gera este JSON
 * ```
 * SELECT
 *   id, home_score, away_score, status, minute_played,
 *   interrupted_reason, scheduled_at, competition_id, competition, round,
 *   home_team:teams!home_team_id(id, name, short_name, logo_url),
 *   away_team:teams!away_team_id(id, name, short_name, logo_url)
 * FROM matches
 * WHERE competition_id = $1
 * ORDER BY scheduled_at ASC
 * ```
 *
 * ## Mapeamento de `GameStatus`
 * O campo [status] (TEXT no banco) mapeia para a sealed class de domínio:
 * - `"scheduled"`  → `GameStatus.Scheduled`
 * - `"live"`       → `GameStatus.Live(minutePlayed!!)`
 * - `"halftime"`   → `GameStatus.HalfTime`
 * - `"finished"`   → `GameStatus.Finished`
 * - `"interrupted"`→ `GameStatus.Interrupted(interruptedReason ?: "Suspenso")`
 *
 * Os campos [minutePlayed] e [interruptedReason] são `null` para estados
 * que não os utilizam — o DDL tem um CHECK CONSTRAINT para garantir isso.
 *
 * ## Eventos Realtime
 * Os eventos da subscription Realtime (INSERT/UPDATE) retornam apenas
 * o registro da tabela `matches` sem os times (join não é transportado).
 * Por isso, [MatchRepositoryImpl] faz um re-fetch completo após qualquer evento.
 */
@Serializable
data class MatchDto(
    @SerialName("id")                  val id: String,

    // Times embutidos via join PostgREST — o alias deve bater com @SerialName
    @SerialName("home_team")           val homeTeam: TeamDto,
    @SerialName("away_team")           val awayTeam: TeamDto,

    // Placar — null antes do início da partida
    @SerialName("home_score")          val homeScore: Int? = null,
    @SerialName("away_score")          val awayScore: Int? = null,

    // Odds e Multiplicador (Nullable pois podem não estar disponíveis ainda)
    @SerialName("home_odd")            val homeOdd: Double? = null,
    @SerialName("draw_odd")            val drawOdd: Double? = null,
    @SerialName("away_odd")            val awayOdd: Double? = null,
    @SerialName("stage_multiplier")    val stageMultiplier: Int = 1,

    // Status e dados auxiliares do estado
    @SerialName("status")              val status: String = "scheduled",
    @SerialName("minute_played")       val minutePlayed: Int? = null,
    @SerialName("interrupted_reason")  val interruptedReason: String? = null,

    // Data/hora no formato ISO 8601 com timezone (TIMESTAMPTZ do PostgreSQL)
    @SerialName("scheduled_at")        val scheduledAt: String,

    // Metadados da competição
    @SerialName("competition_id")      val competitionId: String,
    @SerialName("competition")         val competition: String,
    @SerialName("round")               val round: String,
)
