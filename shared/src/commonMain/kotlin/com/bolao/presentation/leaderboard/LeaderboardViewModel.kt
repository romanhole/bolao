package com.bolao.presentation.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolao.domain.repository.LeaderboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LeaderboardViewModel(
    private val repository: LeaderboardRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LeaderboardUiState>(LeaderboardUiState.Loading)
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    init {
        loadLeaderboard()
    }

    fun refresh() {
        val currentState = _uiState.value
        if (currentState is LeaderboardUiState.Success) {
            // Se já temos dados, mostra apenas o spinner do Pull-to-Refresh
            _uiState.value = currentState.copy(isRefreshing = true)
        } else {
            // Se falhou antes ou estava carregando, volta pro loading central
            _uiState.value = LeaderboardUiState.Loading
        }
        
        viewModelScope.launch {
            repository.getLeaderboard()
                .onSuccess { items ->
                    _uiState.value = LeaderboardUiState.Success(items = items, isRefreshing = false)
                }
                .onFailure { error ->
                    _uiState.value = LeaderboardUiState.Error(
                        message = error.message ?: "Erro ao recarregar ranking"
                    )
                }
        }
    }

    fun loadLeaderboard() {
        _uiState.value = LeaderboardUiState.Loading
        refresh()
    }
}
