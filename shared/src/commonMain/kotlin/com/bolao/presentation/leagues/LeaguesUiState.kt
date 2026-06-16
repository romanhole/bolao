package com.bolao.presentation.leagues

import com.bolao.domain.model.League

sealed interface LeaguesUiState {
    data object Loading : LeaguesUiState
    data class Success(val leagues: List<League>) : LeaguesUiState
    data class Error(val message: String) : LeaguesUiState
}
