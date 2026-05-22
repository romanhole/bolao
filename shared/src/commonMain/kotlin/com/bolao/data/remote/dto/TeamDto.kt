package com.bolao.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO (Data Transfer Object) para a tabela `teams` do Supabase.
 *
 * ## Convenção de nomes
 * O PostgreSQL usa `snake_case` (ex: `short_name`, `logo_url`).
 * O domínio Kotlin usa `camelCase` (ex: `shortName`, `logoUrl`).
 * O `@SerialName` faz a ponte sem poluir a camada de domínio.
 *
 * Este DTO é usado tanto em selects diretos (`from("teams")`) quanto
 * em joins embutidos no select de `matches`:
 * ```
 * home_team:teams!home_team_id(id, name, short_name, logo_url)
 * ```
 * O alias `home_team` no PostgREST corresponde ao `@SerialName("home_team")`
 * em [MatchDto].
 */
@Serializable
data class TeamDto(
    @SerialName("id")         val id: String,
    @SerialName("name")       val name: String,
    @SerialName("short_name") val shortName: String,
    @SerialName("logo_url")   val logoUrl: String = "",
    @SerialName("api_team_id") val apiTeamId: String? = null,
)
