package com.bolao.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO para a tabela `predictions` do Supabase.
 *
 * ## Upsert
 * Para salvar um palpite, usamos `upsert` com `ON CONFLICT (match_id, user_id)`.
 * O [id] é opcional no envio (null para novos palpites — o banco gera via UUID).
 * O [pointsEarned] nunca é enviado pelo cliente — é calculado exclusivamente
 * pelo backend (função Edge Function ou trigger PostgreSQL) após o jogo encerrar.
 *
 * ## Realtime
 * Após o backend calcular os pontos, ele atualiza a coluna `points_earned`.
 * Isso dispara um evento Realtime que re-emite a lista de palpites com os pontos.
 */
@Serializable
data class PredictionDto(
    @SerialName("id")             val id: String? = null,
    @SerialName("match_id")       val matchId: String,
    @SerialName("user_id")        val userId: String,
    @SerialName("predicted_home") val predictedHome: Int,
    @SerialName("predicted_away") val predictedAway: Int,
    // null → jogo ainda não encerrou; calculado pelo backend
    @SerialName("points_earned")  val pointsEarned: Int? = null,
)
