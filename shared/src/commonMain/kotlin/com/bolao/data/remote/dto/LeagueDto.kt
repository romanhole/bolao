package com.bolao.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LeagueDto(
    @SerialName("id") val id: String? = null, // Optional for inserts
    @SerialName("name") val name: String,
    @SerialName("invite_code") val inviteCode: String,
    @SerialName("owner_id") val ownerId: String,
)
