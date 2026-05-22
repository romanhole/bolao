package com.bolao.presentation.leagues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolao.domain.repository.LeagueRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LeaguesViewModel(
    private val repository: LeagueRepository
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(Unit)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<LeaguesUiState> = refreshTrigger
        .flatMapLatest {
            repository.getUserLeagues()
                .map { LeaguesUiState.Success(it) as LeaguesUiState }
                .catch { emit(LeaguesUiState.Error(it.message ?: "Erro ao carregar ligas")) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LeaguesUiState.Loading
        )

    fun loadLeagues() {
        refreshTrigger.value = Unit
    }

    // Eventos tipados: ShowMessage (Snackbar) ou NavigateToLeague (navegar)
    private val _events = MutableSharedFlow<LeagueEvent>()
    val events = _events.asSharedFlow()

    fun createLeague(name: String, nickname: String) {
        if (name.isBlank() || nickname.isBlank()) return
        viewModelScope.launch {
            repository.createLeague(name, nickname)
                .onSuccess { league ->
                    loadLeagues()
                    // Navega diretamente para a liga criada (fecha o dialog e vai para detalhes)
                    _events.emit(LeagueEvent.NavigateToLeague(league.id))
                }
                .onFailure { error ->
                    _events.emit(LeagueEvent.ShowMessage("Erro ao criar liga: ${error.message}"))
                }
        }
    }

    fun joinLeague(inviteCode: String, nickname: String) {
        if (inviteCode.isBlank() || nickname.isBlank()) return
        viewModelScope.launch {
            repository.joinLeague(inviteCode.trim(), nickname.trim())
                .onSuccess {
                    loadLeagues()
                    _events.emit(LeagueEvent.ShowMessage("Você entrou na liga com sucesso!"))
                }
                .onFailure { error ->
                    _events.emit(LeagueEvent.ShowMessage("Erro: ${error.message}"))
                }
        }
    }
}

