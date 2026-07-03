package com.bolao.presentation.matchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolao.domain.model.League
import com.bolao.domain.model.Prediction
import com.bolao.domain.repository.AuthRepository
import com.bolao.domain.repository.LeaderboardRepository
import com.bolao.domain.repository.LeagueRepository
import com.bolao.domain.repository.MatchRepository
import com.bolao.domain.repository.PredictionRepository
import com.bolao.domain.usecase.PredictionCalculator
import com.bolao.presentation.leagues.LiveMatchUserScore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel da tela principal de palpites.
 *
 * ## Padrão de edição local ("draft")
 * O usuário pode alterar os palpites com os botões +/- sem salvar imediatamente.
 * As edições ficam em [_draftEdits] (map em memória). Ao clicar em "Confirmar",
 * [savePrediction] persiste o draft no backend via [PredictionRepository].
 *
 * ## Autenticação
 * O userId real é obtido de [AuthRepository.currentUser] no [init].
 * Como [App] garante que este ViewModel só existe quando o usuário está autenticado,
 * `filterNotNull().first()` resolve imediatamente com o usuário da sessão ativa.
 *
 * ## Realtime
 * O WebSocket já está conectado globalmente (feito no NetworkModule).
 * Este ViewModel não gerencia connect/disconnect — apenas coleta os flows reativos.
 */
class MatchListViewModel(
    private val matchRepository: MatchRepository,
    private val predictionRepository: PredictionRepository,
    private val authRepository: AuthRepository,
    private val leagueRepository: LeagueRepository,
    private val leaderboardRepository: LeaderboardRepository,
) : ViewModel() {

    companion object {
        // TODO: tornar configurável por tela quando houver seleção de competição
        private const val COMPETITION_ID = "copa_do_mundo_2026"
    }

    private val _uiState = MutableStateFlow<MatchListUiState>(MatchListUiState.Loading)
    val uiState: StateFlow<MatchListUiState> = _uiState.asStateFlow()

    private val _selectedRound = MutableStateFlow<String?>(null)

    /**
     * Edições locais ainda não salvas no backend.
     */
    private data class DraftEdit(val home: Int, val away: Int, val qualifier: String? = null)

    private val _draftEdits = MutableStateFlow<Map<String, DraftEdit>>(emptyMap())

    /** Override local para atualizar a UI imediatamente enquanto o realtime não chega. */
    private val _savedPredictionsOverride = MutableStateFlow<Map<String, Prediction>>(emptyMap())

    /**
     * Metadados de UI por partida (estado do save: salvando / erro).
     * Separado do combine principal para evitar re-emissão desnecessária do flow de dados.
     */
    private val _perMatchMeta = MutableStateFlow<Map<String, PerMatchMeta>>(emptyMap())

    /** Estado interno por partida — isSaving e mensagem de erro. */
    private data class PerMatchMeta(
        val isSaving: Boolean = false,
        val saveError: String? = null,
    )

    private data class StateData(
        val matches: List<com.bolao.domain.model.Match>,
        val savedPredictions: List<Prediction>,
        val drafts: Map<String, DraftEdit>,
        val meta: Map<String, PerMatchMeta>,
        val currentRound: String?
    )

    /**
     * userId do usuário autenticado.
     * Inicializado no [init] antes de observar dados.
     */
    private val _currentUserId = MutableStateFlow<String>("")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    // ── Estado do Bottom Sheet de Palpites do Grupo ──────────────────────────

    private val _showPredictionsSheetForMatchId = MutableStateFlow<String?>(null)
    val showPredictionsSheetForMatchId: StateFlow<String?> = _showPredictionsSheetForMatchId.asStateFlow()

    private val _userLeagues = MutableStateFlow<List<League>>(emptyList())
    val userLeagues: StateFlow<List<League>> = _userLeagues.asStateFlow()

    private val _selectedLeagueId = MutableStateFlow<String?>(null)
    val selectedLeagueId: StateFlow<String?> = _selectedLeagueId.asStateFlow()

    private val _sheetPredictions = MutableStateFlow<List<LiveMatchUserScore>>(emptyList())
    val sheetPredictions: StateFlow<List<LiveMatchUserScore>> = _sheetPredictions.asStateFlow()

    private val _sheetIsLoading = MutableStateFlow(false)
    val sheetIsLoading: StateFlow<Boolean> = _sheetIsLoading.asStateFlow()

    init {
        viewModelScope.launch {
            // Aguarda o userId real — App.kt garante que estamos autenticados aqui
            val user = authRepository.currentUser.filterNotNull().first()
            _currentUserId.value = user.userId

            // Busca as ligas que o usuário participa
            leagueRepository.getUserLeagues().collect { leagues ->
                _userLeagues.value = leagues
            }
        }
        viewModelScope.launch {
            authRepository.currentUser.filterNotNull().first() // apenas para sincronizar
            observeData()
        }
    }

    // ── Observação de dados ────────────────────────────────────────────────────

    /**
     * Combina os 4 flows para construir a lista de [MatchPredictionItem]:
     * - Flow de partidas (Realtime do backend)
     * - Flow de palpites salvos do usuário (Realtime do backend)
     * - Flow de edições locais (em memória)
     * - Flow de estado de save por partida (em memória)
     */
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun observeData() {
        val clockTick = kotlinx.coroutines.flow.flow {
            while (true) {
                emit(kotlinx.datetime.Clock.System.now().toEpochMilliseconds())
                kotlinx.coroutines.delay(5000)
            }
        }

        viewModelScope.launch {
            val combinedData = combine(
                matchRepository.observeMatchesByCompetition(COMPETITION_ID),
                combine(
                    predictionRepository.observePredictionsByUser(_currentUserId.value, COMPETITION_ID),
                    _savedPredictionsOverride
                ) { list, overrides ->
                    val map = list.associateBy { it.matchId }.toMutableMap()
                    map.putAll(overrides)
                    map.values.toList()
                },
                _draftEdits,
                _perMatchMeta,
                _selectedRound
            ) { matches, savedPredictions, drafts, meta, currentRound ->
                StateData(matches, savedPredictions, drafts, meta, currentRound)
            }

            combine(combinedData, clockTick) { data, _ ->
                val matches = data.matches
                val savedPredictions = data.savedPredictions
                val drafts = data.drafts
                val meta = data.meta
                val currentRound = data.currentRound

                val rawRounds = matches.map { it.round }.distinct()
                val orderMap = mapOf(
                    "Fase de Grupos" to 1,
                    "Rodada 1" to 2,
                    "Rodada 2" to 3,
                    "Rodada 3" to 4,
                    "16-avos de final" to 5,
                    "Oitavas de final" to 6,
                    "Quartas de final" to 7,
                    "Semifinais" to 8,
                    "Terceiro Lugar" to 9,
                    "Final" to 10
                )
                val availableRounds = rawRounds.sortedBy { orderMap[it] ?: 99 }

                var activeRound = currentRound
                if (activeRound == null && matches.isNotEmpty()) {
                    val firstRelevant = matches.firstOrNull {
                        it.status is com.bolao.domain.model.GameStatus.Scheduled ||
                            it.status is com.bolao.domain.model.GameStatus.Live
                    }
                    activeRound = firstRelevant?.round ?: availableRounds.firstOrNull()
                    if (activeRound != null) {
                        _selectedRound.value = activeRound
                    }
                }

                val filteredMatches = matches.filter { it.round == activeRound }
                val predByMatchId = savedPredictions.associateBy { it.matchId }

                val items = filteredMatches.map { match ->
                    val saved = predByMatchId[match.id]
                    val draft = drafts[match.id]
                    val savedHome = saved?.predictedHome ?: 0
                    val savedAway = saved?.predictedAway ?: 0
                    val matchMeta = meta[match.id] ?: PerMatchMeta()
                    val hasUnsavedChangesValue = saved == null || (
                        draft != null && (
                            draft.home != savedHome ||
                                draft.away != savedAway ||
                                draft.qualifier != saved.predictedQualifier
                            )
                        )
                    MatchPredictionItem(
                        match = match,
                        savedPrediction = saved,
                        currentHomeGoals = draft?.home ?: savedHome,
                        currentAwayGoals = draft?.away ?: savedAway,
                        currentQualifier = draft?.qualifier ?: saved?.predictedQualifier,
                        hasUnsavedChanges = hasUnsavedChangesValue,
                        isSaving = matchMeta.isSaving,
                        saveError = matchMeta.saveError,
                        isPredictionAllowed = match.isPredictionAllowed,
                    )
                }
                MatchListUiState.Success(items, availableRounds, activeRound)
            }
                .catch { e ->
                    _uiState.value = MatchListUiState.Error(
                        e.message ?: "Erro ao carregar partidas"
                    )
                }
                .collect { state ->
                    if (state is MatchListUiState.Success) {
                        _uiState.value = state
                    }
                }
        }
    }

    // ── Ações do usuário ───────────────────────────────────────────────────────

    fun selectRound(round: String) {
        _selectedRound.value = round
    }

    /**
     * Incrementa ou decrementa os gols do time mandante em [delta] (+1 ou -1).
     * Coerced ao mínimo de 0.
     */
    fun updateHomeGoals(matchId: String, delta: Int) {
        val current = getOrInitDraft(matchId)
        val newHome = (current.home + delta).coerceAtLeast(0)
        _draftEdits.update { it + (matchId to current.copy(home = newHome)) }
    }

    /**
     * Incrementa ou decrementa os gols do time visitante em [delta] (+1 ou -1).
     * Coerced ao mínimo de 0.
     */
    fun updateAwayGoals(matchId: String, delta: Int) {
        val current = getOrInitDraft(matchId)
        val newAway = (current.away + delta).coerceAtLeast(0)
        _draftEdits.update { it + (matchId to current.copy(away = newAway)) }
    }

    /**
     * Atualiza o palpite de qual time se classifica.
     */
    fun updateQualifier(matchId: String, qualifier: String) {
        val current = getOrInitDraft(matchId)
        _draftEdits.update { it + (matchId to current.copy(qualifier = qualifier)) }
    }

    /**
     * Persiste o palpite atual para [matchId] no backend.
     * Usa o [currentUserId] real do usuário autenticado.
     */
    fun savePrediction(matchId: String) {
        // Se o usuário não mexeu nos contadores, o draft será nulo. Assumimos o 0x0 inicial da tela.
        val draft = _draftEdits.value[matchId] ?: DraftEdit(0, 0)

        viewModelScope.launch {
            // Sinaliza "salvando" para desabilitar o botão e mostrar spinner
            _perMatchMeta.update { it + (matchId to PerMatchMeta(isSaving = true)) }

            val prediction = Prediction(
                id = "", // O backend gera o ID via UUID
                matchId = matchId,
                userId = _currentUserId.value, // userId REAL do Supabase Auth
                predictedHome = draft.home,
                predictedAway = draft.away,
                predictedQualifier = draft.qualifier,
            )

            predictionRepository.savePrediction(prediction)
                .onSuccess { savedPred ->
                    // Draft consumido — o flow do backend emitirá o novo estado via Realtime
                    _draftEdits.update { it - matchId }
                    _perMatchMeta.update { it + (matchId to PerMatchMeta()) }
                    // Override local: o usuário vê imediatamente a mudança na UI, sem depender do Realtime!
                    _savedPredictionsOverride.update { it + (matchId to savedPred) }
                }
                .onFailure { error ->
                    _perMatchMeta.update {
                        it + (
                            matchId to PerMatchMeta(
                                saveError = error.message ?: "Erro ao salvar palpite. Tente novamente."
                            )
                            )
                    }
                }
        }
    }

    /** Limpa a mensagem de erro de save para [matchId] (ex: após o usuário dispensar). */
    fun clearSaveError(matchId: String) {
        _perMatchMeta.update { map ->
            val current = map[matchId] ?: return@update map
            map + (matchId to current.copy(saveError = null))
        }
    }

    // ── Auxiliares ────────────────────────────────────────────────────────────

    /**
     * Retorna o draft existente para [matchId], ou inicializa a partir
     * do palpite já salvo (para que o usuário parta do valor que já confirmou).
     */
    @Suppress("ReturnCount")
    private fun getOrInitDraft(matchId: String): DraftEdit {
        _draftEdits.value[matchId]?.let { return it }

        val currentState = _uiState.value
        if (currentState is MatchListUiState.Success) {
            val item = currentState.items.find { it.match.id == matchId }
            val saved = item?.savedPrediction
            return DraftEdit(saved?.predictedHome ?: 0, saved?.predictedAway ?: 0, saved?.predictedQualifier)
        }
        return DraftEdit(0, 0)
    }

    // ── Controle do Bottom Sheet de Palpites do Grupo ─────────────────────────

    fun openPredictionsSheet(matchId: String) {
        _showPredictionsSheetForMatchId.value = matchId
        if (_selectedLeagueId.value == null && _userLeagues.value.isNotEmpty()) {
            _selectedLeagueId.value = _userLeagues.value.first().id
        }
        loadSheetPredictions()
    }

    fun closePredictionsSheet() {
        _showPredictionsSheetForMatchId.value = null
        _sheetPredictions.value = emptyList()
    }

    fun selectLeagueForSheet(leagueId: String) {
        _selectedLeagueId.value = leagueId
        loadSheetPredictions()
    }

    private fun loadSheetPredictions() {
        val matchId = _showPredictionsSheetForMatchId.value ?: return
        val leagueId = _selectedLeagueId.value ?: return

        // Obter o estado atual da partida para usar os placares e odds reativos
        val currentState = _uiState.value
        if (currentState !is MatchListUiState.Success) return
        val match = currentState.items.find { it.match.id == matchId }?.match ?: return

        viewModelScope.launch {
            _sheetIsLoading.value = true

            // 1. Busca os membros da liga selecionada
            val leaderboardResult = leaderboardRepository.getLeaderboard(leagueId)
            val members = leaderboardResult.getOrNull() ?: emptyList()
            if (members.isEmpty()) {
                _sheetPredictions.value = emptyList()
                _sheetIsLoading.value = false
                return@launch
            }

            // 2. Busca os palpites desta partida apenas para estes membros
            val userIds = members.map { it.userId }
            val predictionsResult = predictionRepository.getMatchPredictionsByUsers(matchId, userIds)
            val predictions = predictionsResult.getOrNull() ?: emptyList()

            // 3. Calcula os pontos ganhos
            val scores = members.mapNotNull { member ->
                val pred = predictions.find { it.userId == member.userId } ?: return@mapNotNull null
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
                    userId = member.userId,
                    nickname = member.nickname,
                    predictedHome = pred.predictedHome,
                    predictedAway = pred.predictedAway,
                    predictedQualifier = pred.predictedQualifier,
                    partialPoints = pts
                )
            }.sortedWith(
                compareByDescending<LiveMatchUserScore> { it.partialPoints }
                    .thenBy {
                        val actualHome = match.homeScore ?: 0
                        val actualAway = match.awayScore ?: 0
                        kotlin.math.abs(it.predictedHome - actualHome) + kotlin.math.abs(it.predictedAway - actualAway)
                    }
            )

            _sheetPredictions.value = scores
            _sheetIsLoading.value = false
        }
    }
}
