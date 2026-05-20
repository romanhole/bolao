package com.bolao.domain.model

import kotlinx.serialization.Serializable

/**
 * Entidade de domínio que representa o palpite de um usuário para uma partida.
 *
 * @property id             ID único do palpite no backend.
 * @property matchId        ID da partida a que este palpite pertence.
 * @property userId         ID do usuário que realizou o palpite.
 * @property predictedHome  Gols que o usuário prevê para o time mandante.
 * @property predictedAway  Gols que o usuário prevê para o time visitante.
 * @property pointsEarned   Pontos concedidos após o jogo encerrar (null = jogo em aberto).
 */
@Serializable
data class Prediction(
    val id: String,
    val matchId: String,
    val userId: String,
    val predictedHome: Int,
    val predictedAway: Int,
    val pointsEarned: Int? = null,
) {
    /**
     * Resultado previsto pelo usuário.
     * Útil para comparação com o resultado real sem acesso à entidade [Match].
     */
    val predictedOutcome: PredictionOutcome
        get() = when {
            predictedHome > predictedAway -> PredictionOutcome.HomeWin
            predictedHome < predictedAway -> PredictionOutcome.AwayWin
            else                          -> PredictionOutcome.Draw
        }

    /** Indica se o palpite já foi pontuado. */
    val isScored: Boolean get() = pointsEarned != null

    /** Placar previsto formatado, ex: "2 – 1" */
    val predictedScoreDisplay: String
        get() = "$predictedHome – $predictedAway"
}

/**
 * Resultado previsto: vitória do mandante, empate ou vitória do visitante.
 * Segue a terminologia padrão "1X2" do mercado de apostas esportivas.
 */
@Serializable
enum class PredictionOutcome {
    HomeWin, Draw, AwayWin
}
