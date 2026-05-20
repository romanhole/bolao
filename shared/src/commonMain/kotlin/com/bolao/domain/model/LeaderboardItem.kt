package com.bolao.domain.model

/**
 * Modelo de domínio para um usuário no Ranking (Leaderboard).
 * 
 * @property userId O identificador único do usuário (UUID do Supabase Auth).
 * @property totalPoints Pontuação total somando todos os palpites.
 * @property totalPredictionsMade Quantidade de palpites feitos por este usuário.
 * @property exactMatches Quantidade de placares cravados exatos. Usado como critério de desempate.
 */
data class LeaderboardItem(
    val userId: String,
    val totalPoints: Int,
    val totalPredictionsMade: Int,
    val exactMatches: Int,
)
