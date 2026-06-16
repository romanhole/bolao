package com.bolao.presentation.matchlist

import com.bolao.domain.model.Match
import com.bolao.domain.model.Prediction

/**
 * Representa um item na lista de partidas: a partida combinada com o estado
 * do palpite do usuário (salvo + edições locais ainda não confirmadas).
 *
 * @property match              A partida a ser exibida.
 * @property savedPrediction    O último palpite salvo no backend, se houver.
 * @property currentHomeGoals   Gols do mandante atualmente exibidos (pode ser draft).
 * @property currentAwayGoals   Gols do visitante atualmente exibidos (pode ser draft).
 * @property hasUnsavedChanges  True se o usuário editou mas ainda não salvou.
 * @property isSaving           True enquanto uma operação de save está em progresso.
 * @property saveError          Mensagem de erro do último save, ou null.
 */
data class MatchPredictionItem(
    val match: Match,
    val savedPrediction: Prediction?,
    val currentHomeGoals: Int,
    val currentAwayGoals: Int,
    val hasUnsavedChanges: Boolean,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val isPredictionAllowed: Boolean = false,
)

/**
 * Estado global da MatchListScreen.
 * Sealed class garante exhaustive-check no `when` da UI.
 */
sealed class MatchListUiState {
    /** Carregamento inicial — exibir skeleton ou spinner. */
    data object Loading : MatchListUiState()

    /** Dados prontos. [items] pode estar vazio. */
    data class Success(
        val items: List<MatchPredictionItem>,
        val availableRounds: List<String> = emptyList(),
        val selectedRound: String? = null
    ) : MatchListUiState()

    /** Falha irrecuperável ao carregar dados do backend. */
    data class Error(val message: String) : MatchListUiState()
}
