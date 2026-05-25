package com.bolao.presentation.leagues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolao.domain.model.LeaderboardItem
import com.bolao.domain.model.League
import com.bolao.domain.repository.LeaderboardRepository
import com.bolao.domain.repository.LeagueRepository
import com.bolao.domain.repository.MatchRepository
import com.bolao.domain.repository.PredictionRepository
import com.bolao.domain.usecase.PredictionCalculator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

import com.bolao.domain.model.Match

data class LiveMatchUserScore(
    val userId: String,
    val nickname: String,
    val predictedHome: Int,
    val predictedAway: Int,
    val partialPoints: Int
)

data class LiveMatchDetail(
    val match: Match,
    val partialRanking: List<LiveMatchUserScore>
)

sealed interface LeagueDetailUiState {
    data object Loading : LeagueDetailUiState
    data class Success(
        val league: League,
        val ranking: List<LeaderboardItem>,
        val liveMatchesDetails: List<LiveMatchDetail> = emptyList(),
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
    private val matchRepository: MatchRepository,
    private val predictionRepository: PredictionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LeagueDetailUiState>(LeagueDetailUiState.Loading)
    val uiState: StateFlow<LeagueDetailUiState> = _uiState.asStateFlow()

    private var currentLeagueId: String? = null
    private var liveRankingJob: Job? = null

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

        liveRankingJob?.cancel()

        liveRankingJob = viewModelScope.launch {
            val leagueResult = leagueRepository.getLeagueById(leagueId)
            val leaderboardResult = leaderboardRepository.getLeaderboard(leagueId)

            if (leagueResult.isSuccess && leaderboardResult.isSuccess) {
                val league = leagueResult.getOrThrow()
                val baseLeaderboard = leaderboardResult.getOrThrow()
                
                // Busca todos os palpites dos membros desta liga
                val userIds = baseLeaderboard.map { it.userId }
                val predictionsResult = predictionRepository.getPredictionsForUsers(userIds)
                val allPredictions = predictionsResult.getOrNull() ?: emptyList()

                // Observa partidas ao vivo para somar pontos reativos na memória
                matchRepository.observeMatchesByCompetition("league_22")
                    .map { matches ->
                        val liveMatches = matches.filter { it.status is com.bolao.domain.model.GameStatus.Live }
                        
                        if (liveMatches.isEmpty()) {
                            // Se não há jogos ao vivo, o ranking base é absoluto
                            LeagueDetailUiState.Success(league, baseLeaderboard)
                        } else {
                            // Existem jogos ao vivo! Recalcula o placar reativo
                            val liveDetails = liveMatches.map { match ->
                                val partials = baseLeaderboard.mapNotNull { item ->
                                    val pred = allPredictions.find { it.userId == item.userId && it.matchId == match.id }
                                    if (pred != null) {
                                        val pts = PredictionCalculator.calculateEarnedPoints(
                                            predHome = pred.predictedHome,
                                            predAway = pred.predictedAway,
                                            actualHome = match.homeScore ?: 0,
                                            actualAway = match.awayScore ?: 0,
                                            stageMultiplier = match.stageMultiplier,
                                            homeOdd = match.homeOdd,
                                            drawOdd = match.drawOdd,
                                            awayOdd = match.awayOdd
                                        )
                                        LiveMatchUserScore(
                                            userId = item.userId,
                                            nickname = item.nickname,
                                            predictedHome = pred.predictedHome,
                                            predictedAway = pred.predictedAway,
                                            partialPoints = pts
                                        )
                                    } else null
                                }.sortedByDescending { it.partialPoints }
                                LiveMatchDetail(match, partials)
                            }

                            val updatedRanking = baseLeaderboard.map { item ->
                                val livePoints = liveDetails.sumOf { detail ->
                                    detail.partialRanking.find { it.userId == item.userId }?.partialPoints ?: 0
                                }
                                item.copy(totalPoints = item.totalPoints + livePoints)
                            }.sortedByDescending { it.totalPoints }

                            LeagueDetailUiState.Success(league, updatedRanking, liveDetails)
                        }
                    }
                    .collect { newState ->
                        _uiState.value = newState
                    }
            } else {
                val error = leagueResult.exceptionOrNull() ?: leaderboardResult.exceptionOrNull()
                _uiState.value = LeagueDetailUiState.Error(
                    message = error?.message ?: "Erro ao carregar a liga."
                )
            }
        }
    }
}
