package com.bolao.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO para inserir e ler registros na tabela `public.profiles`.
 * Criado logo após o signUp para associar o nickname ao user_id.
 */
@Serializable
data class ProfileDto(
    @SerialName("user_id") val userId: String,
    @SerialName("nickname") val nickname: String,
)
