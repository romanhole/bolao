package com.bolao.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO para mapear o retorno da view `leaderboard` do Supabase.
 * A anotação @SerialName traduz do padrão snake_case do Postgres para camelCase no Kotlin.
 */
@Serializable
data class LeaderboardDto(
    @SerialName("user_id") val userId: String,
    @SerialName("total_points") val totalPoints: Int,
    @SerialName("total_predictions_made") val totalPredictionsMade: Int,
    @SerialName("exact_matches") val exactMatches: Int,
)
