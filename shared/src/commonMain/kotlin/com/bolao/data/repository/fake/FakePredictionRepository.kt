package com.bolao.data.repository.fake

import com.bolao.domain.model.Prediction
import com.bolao.domain.repository.PredictionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Implementação fake de [PredictionRepository] para desenvolvimento.
 *
 * Persiste palpites em memória com [MutableStateFlow], permitindo que
 * a UI reaja imediatamente após um save — exatamente como faria com
 * Firestore (addSnapshotListener) ou Supabase (realtime subscription).
 *
 * O palpite pré-carregado para match_3 simula um palpite já pontuado
 * (partida encerrada), cobrindo o branch de exibição de pontos.
 *
 * TODO: substituir por [FirebasePredictionRepository] ou similar.
 */
class FakePredictionRepository : PredictionRepository {

    private val _predictions = MutableStateFlow(
        listOf(
            // Palpite já pontuado para a partida encerrada (match_3)
            Prediction(
                id            = "pred_match3_user",
                matchId       = "match_3",
                userId        = "user_current",
                predictedHome = 0,
                predictedAway = 2,
                pointsEarned  = 5,  // acertou o vencedor e ficou perto do placar
            ),
        )
    )

    override fun observePredictionsByUser(
        userId: String,
        competitionId: String,
    ): Flow<List<Prediction>> = _predictions.map { list ->
        list.filter { it.userId == userId }
    }

    override fun observePredictionForMatch(
        userId: String,
        matchId: String,
    ): Flow<Prediction?> = _predictions.map { list ->
        list.find { it.userId == userId && it.matchId == matchId }
    }

    override suspend fun savePrediction(prediction: Prediction): Result<Prediction> {
        return try {
            val saved = prediction.copy(
                id = "pred_${prediction.matchId}_${prediction.userId}"
            )
            // Remove palpite anterior para a mesma partida/usuário e adiciona o novo
            _predictions.value = _predictions.value
                .filterNot { it.matchId == prediction.matchId && it.userId == prediction.userId }
                .plus(saved)
            Result.success(saved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeLeaderboard(competitionId: String): Flow<List<Prediction>> =
        _predictions
}
