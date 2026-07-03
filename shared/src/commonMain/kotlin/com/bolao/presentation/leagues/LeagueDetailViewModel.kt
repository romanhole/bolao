package com.bolao.presentation.leagues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolao.domain.model.LeaderboardItem
import com.bolao.domain.model.League
import com.bolao.domain.model.Match
import com.bolao.domain.repository.AuthRepository
import com.bolao.domain.repository.LeaderboardRepository
import com.bolao.domain.repository.LeagueRepository
import com.bolao.domain.repository.MatchRepository
import com.bolao.domain.repository.PredictionRepository
import com.bolao.domain.usecase.PredictionCalculator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class LiveMatchUserScore(
    val userId: String,
    val nickname: String,
    val predictedHome: Int,
    val predictedAway: Int,
    val predictedQualifier: String?,
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
        val currentUserId: String? = null,
        val liveMatchesDetails: List<LiveMatchDetail> = emptyList(),
        val isRefreshing: Boolean = false,
    ) : LeagueDetailUiState
    data class Error(val message: String) : LeagueDetailUiState
}

/**
 * ViewModel para a tela de detalhes de uma liga.
 */
class LeagueDetailViewModel(
    private val authRepository: AuthRepository,
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

    @Suppress("LongMethod")
    fun loadLeagueDetail(leagueId: String, silent: Boolean = false) {
        currentLeagueId = leagueId
        if (!silent) _uiState.value = LeagueDetailUiState.Loading

        liveRankingJob?.cancel()

        liveRankingJob = viewModelScope.launch {
            val leagueResult = leagueRepository.getLeagueById(leagueId)
            val leaderboardResult = leaderboardRepository.getLeaderboard(leagueId)

            if (leagueResult.isSuccess && leaderboardResult.isSuccess) {
                val league = leagueResult.getOrThrow()
                val baseLeaderboard = leaderboardResult.getOrThrow().sortedWith(
                    compareByDescending<LeaderboardItem> { it.totalPoints }
                        .thenByDescending { it.exactMatches }
                )

                // Pega o id do usuário atual
                var currentUserId: String? = null
                authRepository.currentUser.collect { session ->
                    currentUserId = session?.userId

                    val userIds = baseLeaderboard.map { it.userId }

                    // Observa partidas ao vivo para somar pontos reativos na memória
                    matchRepository.observeMatchesByCompetition("copa_do_mundo_2026")
                        .map { matches ->
                            val liveMatches = matches.filter {
                                it.status is com.bolao.domain.model.GameStatus.Live || it.status is com.bolao.domain.model.GameStatus.HalfTime
                            }

                            if (liveMatches.isEmpty()) {
                                // Se não há jogos ao vivo, o ranking base é absoluto
                                LeagueDetailUiState.Success(league, baseLeaderboard, currentUserId)
                            } else {
                                // Existem jogos ao vivo! Recalcula o placar reativo
                                val liveDetails = liveMatches.map { match ->
                                    // Busca os palpites ESPECÍFICOS para esta partida
                                    val matchPredictionsResult = predictionRepository.getMatchPredictionsByUsers(
                                        matchId = match.id,
                                        userIds = userIds
                                    )
                                    val matchPredictions = matchPredictionsResult.getOrNull() ?: emptyList()

                                    val partials = baseLeaderboard.mapNotNull { item ->
                                        val pred = matchPredictions.find { it.userId == item.userId }
                                        if (pred != null) {
                                            val calculatedQualifier = if (match.homeScore90 == match.awayScore90 && match.isKnockout) {
                                                when {
                                                    match.penaltyWinner == "home" -> "home"
                                                    match.penaltyWinner == "away" -> "away"
                                                    (match.homeScoreEt ?: 0) > (match.awayScoreEt ?: 0) -> "home"
                                                    (match.awayScoreEt ?: 0) > (match.homeScoreEt ?: 0) -> "away"
                                                    (match.homeScore ?: 0) > (match.awayScore ?: 0) -> "home"
                                                    (match.awayScore ?: 0) > (match.homeScore ?: 0) -> "away"
                                                    else -> null
                                                }
                                            } else null

                                            val pts = PredictionCalculator.calculateEarnedPoints(
                                                predHome = pred.predictedHome,
                                                predAway = pred.predictedAway,
                                                actualHome90 = match.homeScore90 ?: match.homeScore ?: 0,
                                                actualAway90 = match.awayScore90 ?: match.awayScore ?: 0,
                                                stageMultiplier = match.stageMultiplier,
                                                homeOdd = match.homeOdd,
                                                drawOdd = match.drawOdd,
                                                awayOdd = match.awayOdd,
                                                isKnockout = match.isKnockout,
                                                predictedQualifier = pred.predictedQualifier,
                                                actualQualifier = calculatedQualifier,
                                                homeTeamId = match.homeTeam.id,
                                                awayTeamId = match.awayTeam.id,
                                            )
                                            LiveMatchUserScore(
                                                userId = item.userId,
                                                nickname = item.nickname,
                                                predictedHome = pred.predictedHome,
                                                predictedAway = pred.predictedAway,
                                                predictedQualifier = pred.predictedQualifier,
                                                partialPoints = pts
                                            )
                                        } else {
                                            null
                                        }
                                    }.sortedByDescending { it.partialPoints }
                                    LiveMatchDetail(match, partials)
                                }

                                val updatedRanking = baseLeaderboard.map { item ->
                                    val livePoints = liveDetails.sumOf { detail ->
                                        detail.partialRanking.find { it.userId == item.userId }?.partialPoints ?: 0
                                    }
                                    item.copy(totalPoints = item.totalPoints + livePoints)
                                }.sortedWith(
                                    compareByDescending<LeaderboardItem> { it.totalPoints }
                                        .thenByDescending { it.exactMatches }
                                )

                                LeagueDetailUiState.Success(league, updatedRanking, currentUserId, liveDetails)
                            }
                        }
                        .collect { newState ->
                            _uiState.value = newState
                        }
                }
            } else {
                val error = leagueResult.exceptionOrNull() ?: leaderboardResult.exceptionOrNull()
                _uiState.value = LeagueDetailUiState.Error(
                    message = error?.message ?: "Erro ao carregar a liga."
                )
            }
        }
    }

    private val _events = MutableSharedFlow<LeagueDetailEvent>()
    val events = _events.asSharedFlow()

    fun removeMember(userId: String) {
        val leagueId = currentLeagueId ?: return
        viewModelScope.launch {
            leagueRepository.removeMember(leagueId, userId)
                .onSuccess {
                    _events.emit(LeagueDetailEvent.ShowMessage("Membro removido com sucesso."))
                    _events.emit(LeagueDetailEvent.SuggestRenewCode)
                    loadLeagueDetail(leagueId, silent = true)
                }
                .onFailure {
                    _events.emit(LeagueDetailEvent.ShowMessage("Erro ao remover membro: ${it.message}"))
                }
        }
    }

    fun renewInviteCode() {
        val leagueId = currentLeagueId ?: return
        viewModelScope.launch {
            leagueRepository.renewInviteCode(leagueId)
                .onSuccess { newCode ->
                    _events.emit(LeagueDetailEvent.ShowMessage("Novo código gerado: $newCode"))
                    loadLeagueDetail(leagueId, silent = true)
                }
                .onFailure {
                    _events.emit(LeagueDetailEvent.ShowMessage("Erro ao renovar código: ${it.message}"))
                }
        }
    }
}

sealed interface LeagueDetailEvent {
    data class ShowMessage(val message: String) : LeagueDetailEvent
    data object SuggestRenewCode : LeagueDetailEvent
}
