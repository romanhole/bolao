package com.bolao.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Representa o status atual de uma partida de futebol.
 *
 * Esta sealed class garante exhaustive-check em `when` expressions,
 * eliminando o risco de status não tratado na UI ou nas regras de negócio.
 */
@Serializable
sealed class GameStatus {

    /** Partida ainda não começou. */
    @Serializable
    data object Scheduled : GameStatus()

    /** Partida em andamento. [minutePlayed] é o minuto atual do jogo. */
    @Serializable
    data class Live(val minutePlayed: Int) : GameStatus()

    /** Intervalo (half-time). */
    @Serializable
    data object HalfTime : GameStatus()

    /** Partida encerrada com resultado definitivo. */
    @Serializable
    data object Finished : GameStatus()

    /** Partida adiada, cancelada ou suspensa. */
    @Serializable
    data class Interrupted(val reason: String) : GameStatus()
}
