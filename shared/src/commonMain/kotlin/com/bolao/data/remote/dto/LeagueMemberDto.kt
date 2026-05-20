package com.bolao.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LeagueMemberDto(
    @SerialName("id") val id: String? = null,
    @SerialName("league_id") val leagueId: String,
    @SerialName("user_id") val userId: String,
)
