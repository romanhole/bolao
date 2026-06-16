package com.bolao.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO para mapear o retorno da view `leaderboard` do Supabase.
 * A view faz JOIN com `profiles` para trazer o nickname.
 */
@Serializable
data class LeaderboardDto(
    @SerialName("league_id") val leagueId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("nickname") val nickname: String,
    @SerialName("total_points") val totalPoints: Int,
    @SerialName("total_predictions_made") val totalPredictionsMade: Int,
    @SerialName("exact_matches") val exactMatches: Int,
)
