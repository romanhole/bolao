package com.bolao.data.remote.mapper

import com.bolao.data.remote.dto.PredictionDto
import com.bolao.domain.model.Prediction

// ═══════════════════════════════════════════════════════════════════════════════
// PredictionDto ↔ Prediction (domínio)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Converte [PredictionDto] → [Prediction] de domínio.
 *
 * O [id] do banco é uma `String?` no DTO (null para novos registros antes do upsert).
 * Mapeamos para String vazia quando null — a entidade de domínio nunca precisará
 * lidar com null nesta posição, pois [Prediction]s com id vazio só existem
 * temporariamente na camada de dados.
 */
fun PredictionDto.toDomain(): Prediction = Prediction(
    id            = id.orEmpty(),
    matchId       = matchId,
    userId        = userId,
    predictedHome = predictedHome,
    predictedAway = predictedAway,
    pointsEarned  = pointsEarned,
)

/**
 * Converte [Prediction] de domínio → [PredictionDto] para enviar ao Supabase.
 *
 * ## Regras de conversão
 * - [id]: convertemos String vazia de volta para `null` para que o PostgREST
 *   não tente inserir um UUID vazio — o banco gera o ID automaticamente.
 * - [pointsEarned]: incluído no DTO, mas o Supabase ignora via RLS/policies.
 *   O backend calcula os pontos; o cliente nunca deve alterar este campo.
 */
fun Prediction.toDto(): PredictionDto = PredictionDto(
    id            = id.ifEmpty { null },
    matchId       = matchId,
    userId        = userId,
    predictedHome = predictedHome,
    predictedAway = predictedAway,
    pointsEarned  = pointsEarned,
)
