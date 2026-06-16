package com.bolao.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SettingsDto(
    @SerialName("id")
    val id: Int,

    @SerialName("min_version_code")
    val minVersionCode: Int,

    @SerialName("latest_version_code")
    val latestVersionCode: Int,
)
