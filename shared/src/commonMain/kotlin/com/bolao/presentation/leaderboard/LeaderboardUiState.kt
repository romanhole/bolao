package com.bolao.presentation.leaderboard

import com.bolao.domain.model.LeaderboardItem

sealed interface LeaderboardUiState {
    data object Loading : LeaderboardUiState
    data class Success(
        val items: List<LeaderboardItem>,
        val isRefreshing: Boolean = false
    ) : LeaderboardUiState
    data class Error(val message: String) : LeaderboardUiState
}
