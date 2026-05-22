package com.bolao.data.remote.mapper

import com.bolao.data.remote.dto.MatchDto
import com.bolao.data.remote.dto.TeamDto
import com.bolao.domain.model.GameStatus
import com.bolao.domain.model.Match
import com.bolao.domain.model.Team
import kotlinx.datetime.Instant

// ═══════════════════════════════════════════════════════════════════════════════
// MatchDto → Match (domínio)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Converte [MatchDto] para a entidade de domínio [Match].
 *
 * A conversão é uma função de extensão pura (sem side effects), tornando-a
 * facilmente testável de forma isolada.
 *
 * ## Instant.parse()
 * O Supabase retorna `TIMESTAMPTZ` no formato ISO 8601:
 * `"2026-06-12T19:00:00+00:00"` ou `"2026-06-12T19:00:00Z"`.
 * `Instant.parse()` do `kotlinx-datetime` suporta ambos os formatos.
 */
fun MatchDto.toDomain(): Match = Match(
    id          = id,
    homeTeam    = homeTeam.toDomain(),
    awayTeam    = awayTeam.toDomain(),
    homeScore   = homeScore,
    awayScore   = awayScore,
    status      = toGameStatus(),
    scheduledAt = Instant.parse(scheduledAt),
    competition = competition,
    round       = round,
    homeOdd     = homeOdd,
    drawOdd     = drawOdd,
    awayOdd     = awayOdd,
    stageMultiplier = stageMultiplier,
)

/**
 * Mapeia o campo [MatchDto.status] (TEXT) para a sealed class [GameStatus].
 *
 * Usa os campos auxiliares [MatchDto.minutePlayed] e [MatchDto.interruptedReason]
 * para construir os variants que carregam dados adicionais.
 *
 * O `else → GameStatus.Scheduled` é o fallback defensivo: se o banco
 * retornar um status desconhecido (ex: erro de migração), tratamos como
 * "agendada" em vez de lançar exceção.
 */
private fun MatchDto.toGameStatus(): GameStatus = when (status) {
    "live"        -> GameStatus.Live(minutePlayed = minutePlayed ?: 0)
    "halftime"    -> GameStatus.HalfTime
    "finished"    -> GameStatus.Finished
    "interrupted" -> GameStatus.Interrupted(reason = interruptedReason ?: "Suspenso")
    else          -> GameStatus.Scheduled
}

// ═══════════════════════════════════════════════════════════════════════════════
// TeamDto → Team (domínio)
// ═══════════════════════════════════════════════════════════════════════════════

fun TeamDto.toDomain(): Team = Team(
    id        = id,
    name      = name,
    shortName = shortName,
    logoUrl   = logoUrl,
    apiTeamId = apiTeamId,
)
