package com.bolao.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PredictionCalculatorTest {

    @Test
    fun testBasePoints_ExactScore_Returns5Points() {
        // Palpite: 2x1, Placar: 2x1 (Exato = 1 tendência + 2 mandante + 2 visitante)
        val points = PredictionCalculator.calculateEarnedPoints(
            predHome = 2, predAway = 1, actualHome = 2, actualAway = 1,
            stageMultiplier = 1, homeOdd = 1.5, drawOdd = 3.5, awayOdd = 5.0
        )
        // Expected: 5 base + 0 zebra (odd 1.5 < 3.0) = 5
        assertEquals(5, points)
    }

    @Test
    fun testBasePoints_TrendAndOneScore_Returns3Points() {
        // Palpite: 3x1, Placar: 2x1 (Acertou Vitória e Gols do Visitante)
        val points = PredictionCalculator.calculateEarnedPoints(
            predHome = 3, predAway = 1, actualHome = 2, actualAway = 1,
            stageMultiplier = 1, homeOdd = 1.5, drawOdd = 3.5, awayOdd = 5.0
        )
        // Expected: 1 tendência + 2 gols visitante = 3
        assertEquals(3, points)
    }

    @Test
    fun testBasePoints_OnlyTrend_Returns1Point() {
        // Palpite: 3x0, Placar: 2x1 (Acertou apenas Vitória)
        val points = PredictionCalculator.calculateEarnedPoints(
            predHome = 3, predAway = 0, actualHome = 2, actualAway = 1,
            stageMultiplier = 1, homeOdd = 1.5, drawOdd = 3.5, awayOdd = 5.0
        )
        // Expected: 1 tendência = 1
        assertEquals(1, points)
    }

    @Test
    fun testBasePoints_WrongTrend_Returns0Points() {
        // Palpite: 1x2, Placar: 2x1 (Errou)
        val points = PredictionCalculator.calculateEarnedPoints(
            predHome = 1, predAway = 2, actualHome = 2, actualAway = 1,
            stageMultiplier = 1, homeOdd = 1.5, drawOdd = 3.5, awayOdd = 5.0
        )
        // Expected: 0
        assertEquals(0, points)
    }

    @Test
    fun testStageMultiplier() {
        // Palpite: 2x1, Placar: 2x1 (5 pts base)
        // Fase: 3x (Multiplier = 3)
        val points = PredictionCalculator.calculateEarnedPoints(
            predHome = 2, predAway = 1, actualHome = 2, actualAway = 1,
            stageMultiplier = 3, homeOdd = 1.5, drawOdd = 3.5, awayOdd = 5.0
        )
        // Expected: (5 base * 3) + 0 zebra = 15
        assertEquals(15, points)
    }

    @Test
    fun testZebraBonus_Draw_Odd3() {
        // Palpite: 1x1, Placar: 1x1 (5 pts base)
        // Empate Odd: 3.5 (faixa >= 3.0 -> +2 zebra)
        val points = PredictionCalculator.calculateEarnedPoints(
            predHome = 1, predAway = 1, actualHome = 1, actualAway = 1,
            stageMultiplier = 1, homeOdd = 2.0, drawOdd = 3.5, awayOdd = 4.0
        )
        // Expected: 5 + 2 = 7
        assertEquals(7, points)
    }

    @Test
    fun testZebraBonus_AwayWin_Odd5() {
        // Palpite: 0x1, Placar: 0x1 (5 pts base)
        // Visitante Odd: 6.0 (faixa >= 5.0 -> +4 zebra)
        val points = PredictionCalculator.calculateEarnedPoints(
            predHome = 0, predAway = 1, actualHome = 0, actualAway = 1,
            stageMultiplier = 1, homeOdd = 1.3, drawOdd = 4.0, awayOdd = 6.0
        )
        // Expected: 5 + 4 = 9
        assertEquals(9, points)
    }

    @Test
    fun testZebraBonus_HomeWin_Odd9() {
        // Palpite: 1x0, Placar: 1x0 (5 pts base)
        // Mandante Odd: 10.0 (faixa >= 9.0 -> +7 zebra)
        val points = PredictionCalculator.calculateEarnedPoints(
            predHome = 1, predAway = 0, actualHome = 1, actualAway = 0,
            stageMultiplier = 1, homeOdd = 10.0, drawOdd = 4.0, awayOdd = 1.1
        )
        // Expected: 5 + 7 = 12
        assertEquals(12, points)
    }

    @Test
    fun testPotentialPoints_SafeNullOdds() {
        // Quando odds são null (antes dos 7 dias), deve retornar null (seguro)
        val potential = PredictionCalculator.calculatePotential(
            predHomeGoals = 1, predAwayGoals = 0, stageMultiplier = 1,
            homeOdd = null, drawOdd = null, awayOdd = null
        )
        assertNull(potential)
    }

    @Test
    fun testPotentialPoints_ZebraIndicator() {
        val potential = PredictionCalculator.calculatePotential(
            predHomeGoals = 0, predAwayGoals = 2, stageMultiplier = 2,
            homeOdd = 1.5, drawOdd = 3.0, awayOdd = 5.5
        )
        // Visitante odd 5.5 (zebra +4)
        // Pontos = (5 * 2) + 4 = 14
        assertTrue(potential!!.isZebra)
        assertEquals(14, potential.maxPotentialPoints)
    }
}
