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
            predHome = 2, predAway = 1, actualHome90 = 2, actualAway90 = 1,
            stageMultiplier = 1.0f, homeOdd = 1.5, drawOdd = 3.5, awayOdd = 5.0
        )
        // Expected: 5 base * 1.0 → ROUND(5.0) + 0 zebra = 5
        assertEquals(5, points)
    }

    @Test
    fun testBasePoints_TrendAndOneScore_Returns3Points() {
        // Palpite: 3x1, Placar: 2x1 (Acertou Vitória e Gols do Visitante)
        val points = PredictionCalculator.calculateEarnedPoints(
            predHome = 3, predAway = 1, actualHome90 = 2, actualAway90 = 1,
            stageMultiplier = 1.0f, homeOdd = 1.5, drawOdd = 3.5, awayOdd = 5.0
        )
        // Expected: 1 tendência + 2 gols visitante = 3
        assertEquals(3, points)
    }

    @Test
    fun testBasePoints_OnlyTrend_Returns1Point() {
        // Palpite: 3x0, Placar: 2x1 (Acertou apenas Vitória)
        val points = PredictionCalculator.calculateEarnedPoints(
            predHome = 3, predAway = 0, actualHome90 = 2, actualAway90 = 1,
            stageMultiplier = 1.0f, homeOdd = 1.5, drawOdd = 3.5, awayOdd = 5.0
        )
        // Expected: 1 tendência = 1
        assertEquals(1, points)
    }

    @Test
    fun testBasePoints_WrongTrend_Returns0Points() {
        // Palpite: 1x2, Placar: 2x1 (Errou)
        val points = PredictionCalculator.calculateEarnedPoints(
            predHome = 1, predAway = 2, actualHome90 = 2, actualAway90 = 1,
            stageMultiplier = 1.0f, homeOdd = 1.5, drawOdd = 3.5, awayOdd = 5.0
        )
        // Expected: 0
        assertEquals(0, points)
    }

    // ── Testes de Multiplicador de Fase ──────────────────────────────────────────

    @Test
    fun testStageMultiplier_1_5x_ExactScore() {
        // Palpite: 2x1, Placar: 2x1 (5 pts base)
        // Fase: Oitavas de Final → 1.5x
        val points = PredictionCalculator.calculateEarnedPoints(
            predHome = 2, predAway = 1, actualHome90 = 2, actualAway90 = 1,
            stageMultiplier = 1.5f, homeOdd = 1.5, drawOdd = 3.5, awayOdd = 5.0
        )
        // Expected: ROUND(5 * 1.5) + 0 zebra = ROUND(7.5) = 8
        assertEquals(8, points)
    }

    @Test
    fun testStageMultiplier_1_5x_OnlyTrend() {
        // Palpite: 3x0, Placar: 2x1 (1 pt base)
        // Fase: Oitavas de Final → 1.5x
        val points = PredictionCalculator.calculateEarnedPoints(
            predHome = 3, predAway = 0, actualHome90 = 2, actualAway90 = 1,
            stageMultiplier = 1.5f, homeOdd = 1.5, drawOdd = 3.5, awayOdd = 5.0
        )
        // Expected: ROUND(1 * 1.5) + 0 zebra = ROUND(1.5) = 2
        assertEquals(2, points)
    }

    @Test
    fun testStageMultiplier_2_0x_ExactScore() {
        // Palpite: 2x1, Placar: 2x1 (5 pts base)
        // Fase: Quartas / Semifinal → 2.0x
        val points = PredictionCalculator.calculateEarnedPoints(
            predHome = 2, predAway = 1, actualHome90 = 2, actualAway90 = 1,
            stageMultiplier = 2.0f, homeOdd = 1.5, drawOdd = 3.5, awayOdd = 5.0
        )
        // Expected: ROUND(5 * 2.0) + 0 zebra = 10
        assertEquals(10, points)
    }

    @Test
    fun testStageMultiplier_2_5x_ExactScore() {
        // Palpite: 2x1, Placar: 2x1 (5 pts base)
        // Fase: Final → 2.5x
        val points = PredictionCalculator.calculateEarnedPoints(
            predHome = 2, predAway = 1, actualHome90 = 2, actualAway90 = 1,
            stageMultiplier = 2.5f, homeOdd = 1.5, drawOdd = 3.5, awayOdd = 5.0
        )
        // Expected: ROUND(5 * 2.5) + 0 zebra = ROUND(12.5) = 13
        assertEquals(13, points)
    }

    @Test
    fun testStageMultiplier_2_5x_OnlyTrend() {
        // Palpite: 3x0, Placar: 2x1 (1 pt base)
        // Fase: Final → 2.5x
        val points = PredictionCalculator.calculateEarnedPoints(
            predHome = 3, predAway = 0, actualHome90 = 2, actualAway90 = 1,
            stageMultiplier = 2.5f, homeOdd = 1.5, drawOdd = 3.5, awayOdd = 5.0
        )
        // Expected: ROUND(1 * 2.5) + 0 zebra = ROUND(2.5) = 3
        assertEquals(3, points)
    }

    // ── Testes de Bônus Zebra ────────────────────────────────────────────────────

    @Test
    fun testZebraBonus_Draw_Odd3() {
        // Palpite: 1x1, Placar: 1x1 (5 pts base)
        // Empate Odd: 3.5 (faixa >= 3.0 → +2 zebra)
        val points = PredictionCalculator.calculateEarnedPoints(
            predHome = 1, predAway = 1, actualHome90 = 1, actualAway90 = 1,
            stageMultiplier = 1.0f, homeOdd = 2.0, drawOdd = 3.5, awayOdd = 4.0
        )
        // Expected: 5 + 2 = 7
        assertEquals(7, points)
    }

    @Test
    fun testZebraBonus_AwayWin_Odd5() {
        // Palpite: 0x1, Placar: 0x1 (5 pts base)
        // Visitante Odd: 6.0 (faixa >= 5.0 → +4 zebra)
        val points = PredictionCalculator.calculateEarnedPoints(
            predHome = 0, predAway = 1, actualHome90 = 0, actualAway90 = 1,
            stageMultiplier = 1.0f, homeOdd = 1.3, drawOdd = 4.0, awayOdd = 6.0
        )
        // Expected: 5 + 4 = 9
        assertEquals(9, points)
    }

    @Test
    fun testZebraBonus_HomeWin_Odd9() {
        // Palpite: 1x0, Placar: 1x0 (5 pts base)
        // Mandante Odd: 10.0 (faixa >= 9.0 → +7 zebra)
        val points = PredictionCalculator.calculateEarnedPoints(
            predHome = 1, predAway = 0, actualHome90 = 1, actualAway90 = 0,
            stageMultiplier = 1.0f, homeOdd = 10.0, drawOdd = 4.0, awayOdd = 1.1
        )
        // Expected: 5 + 7 = 12
        assertEquals(12, points)
    }

    @Test
    fun testZebraBonus_WithMultiplier_1_5x() {
        // Palpite: 1x1, Placar: 1x1 (5 pts base), Fase Oitavas 1.5x, Zebra Odd 3.5 (+2)
        val points = PredictionCalculator.calculateEarnedPoints(
            predHome = 1, predAway = 1, actualHome90 = 1, actualAway90 = 1,
            stageMultiplier = 1.5f, homeOdd = 2.0, drawOdd = 3.5, awayOdd = 4.0
        )
        // Expected: ROUND(5 * 1.5) + 2 zebra = 8 + 2 = 10
        assertEquals(10, points)
    }

    @Test
    fun testZebraBonus_WithMultiplier_2_5x() {
        // Palpite: 0x1, Placar: 0x1 (5 pts base), Fase Final 2.5x, Zebra Odd 6.0 (+4)
        val points = PredictionCalculator.calculateEarnedPoints(
            predHome = 0, predAway = 1, actualHome90 = 0, actualAway90 = 1,
            stageMultiplier = 2.5f, homeOdd = 1.3, drawOdd = 4.0, awayOdd = 6.0
        )
        // Expected: ROUND(5 * 2.5) + 4 zebra = 13 + 4 = 17
        assertEquals(17, points)
    }

    // ── Testes de Potencial ──────────────────────────────────────────────────────

    @Test
    fun testPotentialPoints_SafeNullOdds() {
        // Quando odds são null (antes dos 7 dias), deve retornar null (seguro)
        val potential = PredictionCalculator.calculatePotential(
            predHomeGoals = 1, predAwayGoals = 0, stageMultiplier = 1.0f,
            homeOdd = null, drawOdd = null, awayOdd = null
        )
        assertNull(potential)
    }

    @Test
    fun testPotentialPoints_ZebraIndicator_WithMultiplier() {
        // Fase Oitavas 1.5x, Visitante Odd 5.5 (zebra +4)
        val potential = PredictionCalculator.calculatePotential(
            predHomeGoals = 0, predAwayGoals = 2, stageMultiplier = 1.5f,
            homeOdd = 1.5, drawOdd = 3.0, awayOdd = 5.5
        )
        // Pontos = ROUND(5 * 1.5) + 4 = 8 + 4 = 12
        assertTrue(potential!!.isZebra)
        assertEquals(12, potential.maxPotentialPoints)
    }

    @Test
    fun testPotentialPoints_Final_2_5x_NoZebra() {
        // Fase Final 2.5x, sem zebra (odd < 3.0)
        val potential = PredictionCalculator.calculatePotential(
            predHomeGoals = 1, predAwayGoals = 0, stageMultiplier = 2.5f,
            homeOdd = 1.5, drawOdd = 3.5, awayOdd = 4.5
        )
        // Pontos = ROUND(5 * 2.5) = ROUND(12.5) = 13
        assertFalse(potential!!.isZebra)
        assertEquals(13, potential.maxPotentialPoints)
    }
}
