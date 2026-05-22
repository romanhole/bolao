package com.bolao.presentation.leagues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolao.domain.model.LeaderboardItem
import com.bolao.domain.model.League
import com.bolao.domain.repository.LeaderboardRepository
import com.bolao.domain.repository.LeagueRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LeagueDetailUiState {
    data object Loading : LeagueDetailUiState
    data class Success(
        val league: League,
        val ranking: List<LeaderboardItem>,
        val isRefreshing: Boolean = false,
    ) : LeagueDetailUiState
    data class Error(val message: String) : LeagueDetailUiState
}

/**
 * ViewModel para a tela de detalhes de uma liga.
 */
class LeagueDetailViewModel(
    private val leagueRepository: LeagueRepository,
    private val leaderboardRepository: LeaderboardRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LeagueDetailUiState>(LeagueDetailUiState.Loading)
    val uiState: StateFlow<LeagueDetailUiState> = _uiState.asStateFlow()

    private var currentLeagueId: String? = null

    fun refresh() {
        val id = currentLeagueId ?: return
        val current = _uiState.value
        if (current is LeagueDetailUiState.Success) {
            _uiState.value = current.copy(isRefreshing = true)
        }
        loadLeagueDetail(id, silent = true)
    }

    fun loadLeagueDetail(leagueId: String, silent: Boolean = false) {
        currentLeagueId = leagueId
        if (!silent) _uiState.value = LeagueDetailUiState.Loading

        viewModelScope.launch {
            val leagueResult = leagueRepository.getLeagueById(leagueId)
            val leaderboardResult = leaderboardRepository.getLeaderboard(leagueId)

            if (leagueResult.isSuccess && leaderboardResult.isSuccess) {
                _uiState.value = LeagueDetailUiState.Success(
                    league = leagueResult.getOrThrow(),
                    ranking = leaderboardResult.getOrThrow(),
                )
            } else {
                val error = leagueResult.exceptionOrNull() ?: leaderboardResult.exceptionOrNull()
                _uiState.value = LeagueDetailUiState.Error(
                    message = error?.message ?: "Erro ao carregar a liga."
                )
            }
        }
    }
}
