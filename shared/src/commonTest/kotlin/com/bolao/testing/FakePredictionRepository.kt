package com.bolao.testing

import com.bolao.domain.model.Prediction
import com.bolao.domain.repository.PredictionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Palpites em memória; [savedPredictions] registra cada chamada de save feita pela UI. */
class FakePredictionRepository(initial: List<Prediction> = emptyList()) : PredictionRepository {

    private val predictions = MutableStateFlow(initial)

    val savedPredictions = mutableListOf<Prediction>()

    override fun observePredictionsByUser(userId: String, competitionId: String): Flow<List<Prediction>> =
        predictions.map { list -> list.filter { it.userId == userId } }

    override fun observePredictionForMatch(userId: String, matchId: String): Flow<Prediction?> =
        predictions.map { list -> list.find { it.userId == userId && it.matchId == matchId } }

    override suspend fun savePrediction(prediction: Prediction): Result<Prediction> {
        savedPredictions += prediction
        val saved = prediction.copy(id = "pred_${prediction.matchId}_${prediction.userId}")
        predictions.value = predictions.value
            .filterNot { it.matchId == saved.matchId && it.userId == saved.userId } + saved
        return Result.success(saved)
    }

    override fun observeLeaderboard(competitionId: String): Flow<List<Prediction>> = predictions

    override suspend fun getPredictionsForUsers(userIds: List<String>): Result<List<Prediction>> =
        Result.success(predictions.value.filter { it.userId in userIds })

    override suspend fun getMatchPredictionsByUsers(matchId: String, userIds: List<String>): Result<List<Prediction>> =
        Result.success(predictions.value.filter { it.matchId == matchId && it.userId in userIds })
}
