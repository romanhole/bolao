package com.bolao.domain.usecase

/**
 * Calculadora de pontos isolada na camada de Domínio.
 * Responsável por calcular potenciais de aposta e os pontos reais.
 */
object PredictionCalculator {

    data class PotentialPointsResult(
        val maxPotentialPoints: Int,
        val isZebra: Boolean
    )

    /**
     * Calcula o potencial máximo de pontos para um palpite em tempo real,
     * simulando que o usuário acerte o Placar Exato (5 pts base) + Bônus Zebra.
     * Retorna null se as odds não estiverem disponíveis.
     */
    fun calculatePotential(
        predHomeGoals: Int,
        predAwayGoals: Int,
        stageMultiplier: Int,
        homeOdd: Double?,
        drawOdd: Double?,
        awayOdd: Double?
    ): PotentialPointsResult? {
        if (homeOdd == null || drawOdd == null || awayOdd == null) {
            return null // Odds not available (e.g. frozen 48h not applied yet)
        }

        val predDiff = predHomeGoals - predAwayGoals
        val predSign = if (predDiff > 0) 1 else if (predDiff < 0) -1 else 0
        
        val basePoints = 5 * stageMultiplier
        var zebraBonus = 0
        var isZebra = false
        
        val winningOdd = if (predSign == 1) homeOdd else if (predSign == -1) awayOdd else drawOdd
        if (winningOdd >= 3.0) {
            isZebra = true
            zebraBonus = when {
                winningOdd >= 9.0 -> 7
                winningOdd >= 5.0 -> 4
                winningOdd >= 3.0 -> 2
                else -> 0
            }
        }
        
        return PotentialPointsResult(
            maxPotentialPoints = basePoints + zebraBonus,
            isZebra = isZebra
        )
    }

    /**
     * Calcula os pontos ganhos baseados no resultado real do jogo.
     * Mesma lógica aplicada no backend via Trigger no Supabase.
     */
    fun calculateEarnedPoints(
        predHome: Int,
        predAway: Int,
        actualHome: Int,
        actualAway: Int,
        stageMultiplier: Int,
        homeOdd: Double?,
        drawOdd: Double?,
        awayOdd: Double?
    ): Int {
        val predDiff = predHome - predAway
        val actualDiff = actualHome - actualAway

        val predSign = if (predDiff > 0) 1 else if (predDiff < 0) -1 else 0
        val actualSign = if (actualDiff > 0) 1 else if (actualDiff < 0) -1 else 0

        var basePoints = 0
        if (predSign == actualSign) {
            basePoints = 1 // Tendência (Vencedor ou Empate)
            if (predHome == actualHome) basePoints += 2 // Saldo exato do Mandante
            if (predAway == actualAway) basePoints += 2 // Saldo exato do Visitante
        }

        var zebraBonus = 0
        if (predSign == actualSign) { // Zebra só se acertar a tendência
            val winningOdd = if (actualSign == 1) homeOdd else if (actualSign == -1) awayOdd else drawOdd
            if (winningOdd != null && winningOdd >= 3.0) {
                zebraBonus = when {
                    winningOdd >= 9.0 -> 7
                    winningOdd >= 5.0 -> 4
                    winningOdd >= 3.0 -> 2
                    else -> 0
                }
            }
        }

        return (basePoints * stageMultiplier) + zebraBonus
    }
}
