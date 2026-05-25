package com.bolao.presentation.matchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolao.domain.model.Prediction
import com.bolao.domain.repository.AuthRepository
import com.bolao.domain.repository.MatchRepository
import com.bolao.domain.repository.PredictionRepository
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
) : ViewModel() {

    companion object {
        // TODO: tornar configurável por tela quando houver seleção de competição
        private const val COMPETITION_ID = "brasileirao_2026"
    }

    private val _uiState = MutableStateFlow<MatchListUiState>(MatchListUiState.Loading)
    val uiState: StateFlow<MatchListUiState> = _uiState.asStateFlow()

    private val _selectedRound = MutableStateFlow<String?>(null)

    /**
     * Edições locais ainda não salvas no backend.
     * matchId → Pair(homeGoals, awayGoals)
     */
    private val _draftEdits = MutableStateFlow<Map<String, Pair<Int, Int>>>(emptyMap())

    /**
     * Metadados de UI por partida (estado do save: salvando / erro).
     * Separado do combine principal para evitar re-emissão desnecessária do flow de dados.
     */
    private val _perMatchMeta = MutableStateFlow<Map<String, PerMatchMeta>>(emptyMap())

    /** Estado interno por partida — isSaving e mensagem de erro. */
    private data class PerMatchMeta(
        val isSaving: Boolean  = false,
        val saveError: String? = null,
    )

    /**
     * userId do usuário autenticado.
     * Inicializado no [init] antes de observar dados.
     */
    private var currentUserId: String = ""

    init {
        viewModelScope.launch {
            // Aguarda o userId real — App.kt garante que estamos autenticados aqui
            val user = authRepository.currentUser.filterNotNull().first()
            currentUserId = user.userId
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
    private fun observeData() {
        viewModelScope.launch {
            combine(
                matchRepository.observeMatchesByCompetition(COMPETITION_ID),
                predictionRepository.observePredictionsByUser(currentUserId, COMPETITION_ID),
                _draftEdits,
                _perMatchMeta,
                _selectedRound
            ) { matches, savedPredictions, drafts, meta, currentRound ->
                val availableRounds = matches.map { it.round }.distinct()
                
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
                    val saved     = predByMatchId[match.id]
                    val draft     = drafts[match.id]
                    val savedHome = saved?.predictedHome ?: 0
                    val savedAway = saved?.predictedAway ?: 0
                    val matchMeta = meta[match.id] ?: PerMatchMeta()

                    MatchPredictionItem(
                        match             = match,
                        savedPrediction   = saved,
                        currentHomeGoals  = draft?.first  ?: savedHome,
                        currentAwayGoals  = draft?.second ?: savedAway,
                        hasUnsavedChanges = draft != null &&
                            (draft.first != savedHome || draft.second != savedAway),
                        isSaving          = matchMeta.isSaving,
                        saveError         = matchMeta.saveError,
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
        val newHome = (current.first + delta).coerceAtLeast(0)
        _draftEdits.update { it + (matchId to Pair(newHome, current.second)) }
    }

    /**
     * Incrementa ou decrementa os gols do time visitante em [delta] (+1 ou -1).
     * Coerced ao mínimo de 0.
     */
    fun updateAwayGoals(matchId: String, delta: Int) {
        val current = getOrInitDraft(matchId)
        val newAway = (current.second + delta).coerceAtLeast(0)
        _draftEdits.update { it + (matchId to Pair(current.first, newAway)) }
    }

    /**
     * Persiste o palpite atual para [matchId] no backend.
     * Usa o [currentUserId] real do usuário autenticado.
     */
    fun savePrediction(matchId: String) {
        val draft = _draftEdits.value[matchId] ?: return

        viewModelScope.launch {
            // Sinaliza "salvando" para desabilitar o botão e mostrar spinner
            _perMatchMeta.update { it + (matchId to PerMatchMeta(isSaving = true)) }

            val prediction = Prediction(
                id            = "",             // O backend gera o ID via UUID
                matchId       = matchId,
                userId        = currentUserId,  // userId REAL do Supabase Auth
                predictedHome = draft.first,
                predictedAway = draft.second,
            )

            predictionRepository.savePrediction(prediction)
                .onSuccess {
                    // Draft consumido — o flow do backend emitirá o novo estado via Realtime
                    _draftEdits.update { it - matchId }
                    _perMatchMeta.update { it + (matchId to PerMatchMeta()) }
                }
                .onFailure { error ->
                    _perMatchMeta.update {
                        it + (matchId to PerMatchMeta(
                            saveError = error.message ?: "Erro ao salvar palpite. Tente novamente."
                        ))
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
    private fun getOrInitDraft(matchId: String): Pair<Int, Int> {
        _draftEdits.value[matchId]?.let { return it }

        val currentState = _uiState.value
        if (currentState is MatchListUiState.Success) {
            val item  = currentState.items.find { it.match.id == matchId }
            val saved = item?.savedPrediction
            return Pair(saved?.predictedHome ?: 0, saved?.predictedAway ?: 0)
        }
        return Pair(0, 0)
    }
}
