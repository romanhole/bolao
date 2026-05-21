package com.bolao.presentation.leagues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolao.data.remote.dto.LeaderboardDto
import com.bolao.data.remote.dto.LeagueMemberDto
import com.bolao.domain.model.LeaderboardItem
import com.bolao.domain.model.League
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
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
 *
 * Busca os membros da liga e depois filtra a view de leaderboard
 * apenas pelos user_ids que pertencem àquela liga.
 */
class LeagueDetailViewModel(
    private val supabase: SupabaseClient,
    private val leagueId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LeagueDetailUiState>(LeagueDetailUiState.Loading)
    val uiState: StateFlow<LeagueDetailUiState> = _uiState.asStateFlow()

    init {
        loadLeagueDetail()
    }

    fun refresh() {
        val current = _uiState.value
        if (current is LeagueDetailUiState.Success) {
            _uiState.value = current.copy(isRefreshing = true)
        }
        loadLeagueDetail(silent = true)
    }

    fun loadLeagueDetail(silent: Boolean = false) {
        if (!silent) _uiState.value = LeagueDetailUiState.Loading

        viewModelScope.launch {
            runCatching {
                // 1. Busca os dados da liga
                val leagues = supabase.postgrest["leagues"]
                    .select { filter { eq("id", leagueId) } }
                    .decodeList<com.bolao.data.remote.dto.LeagueDto>()

                val leagueDto = leagues.firstOrNull()
                    ?: throw Exception("Liga não encontrada.")

                val league = League(
                    id = leagueDto.id ?: leagueId,
                    name = leagueDto.name,
                    inviteCode = leagueDto.inviteCode,
                    ownerId = leagueDto.ownerId,
                )

                // 2. Busca os membros da liga
                val members = supabase.postgrest["league_members"]
                    .select { filter { eq("league_id", leagueId) } }
                    .decodeList<LeagueMemberDto>()

                val memberIds = members.map { it.userId }

                // 3. Busca o leaderboard e filtra pelos membros
                val allRanking = supabase.postgrest["leaderboard"]
                    .select()
                    .decodeList<LeaderboardDto>()

                val leagueRanking = allRanking
                    .filter { it.userId in memberIds }
                    .sortedByDescending { it.totalPoints }
                    .map { dto ->
                        LeaderboardItem(
                            userId = dto.userId,
                            nickname = dto.nickname ?: dto.userId.take(8),
                            totalPoints = dto.totalPoints,
                            totalPredictionsMade = dto.totalPredictionsMade,
                            exactMatches = dto.exactMatches,
                        )
                    }

                _uiState.value = LeagueDetailUiState.Success(
                    league = league,
                    ranking = leagueRanking,
                )
            }.onFailure { error ->
                _uiState.value = LeagueDetailUiState.Error(
                    message = error.message ?: "Erro ao carregar a liga."
                )
            }
        }
    }
}
