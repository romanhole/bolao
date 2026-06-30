package com.bolao.domain.model

/**
 * Representa o status atual de uma partida de futebol.
 *
 * Esta sealed class garante exhaustive-check em `when` expressions,
 * eliminando o risco de status não tratado na UI ou nas regras de negócio.
 */
sealed class GameStatus {

    /** Partida ainda não começou. */
    data object Scheduled : GameStatus()

    /** Partida em andamento. [minutePlayed] é o minuto atual do jogo. */
    data class Live(val minutePlayed: Int) : GameStatus()

    /** Intervalo (half-time). */
    data object HalfTime : GameStatus()

    /** Prorrogação em andamento. */
    data class ExtraTime(val minutePlayed: Int) : GameStatus()

    /** Intervalo da prorrogação. */
    data object ExtraTimeHalfTime : GameStatus()

    /** Cobrança de pênaltis. */
    data object Penalties : GameStatus()

    /** Partida encerrada com resultado definitivo. */
    data object Finished : GameStatus()

    /** Partida adiada, cancelada ou suspensa. */
    data class Interrupted(val reason: String) : GameStatus()
}
