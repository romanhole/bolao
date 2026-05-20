package com.bolao.presentation.leagues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bolao.domain.repository.LeagueRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LeaguesViewModel(
    private val repository: LeagueRepository
) : ViewModel() {

    val uiState: StateFlow<LeaguesUiState> = repository.getUserLeagues()
        .map { LeaguesUiState.Success(it) as LeaguesUiState }
        .catch { emit(LeaguesUiState.Error(it.message ?: "Erro ao carregar ligas")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LeaguesUiState.Loading
        )

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    fun createLeague(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createLeague(name)
                .onSuccess {
                    _events.emit("Liga '${it.name}' criada com sucesso! (Código: ${it.inviteCode})")
                }
                .onFailure {
                    _events.emit("Erro ao criar liga: ${it.message}")
                }
        }
    }

    fun joinLeague(inviteCode: String) {
        if (inviteCode.isBlank()) return
        viewModelScope.launch {
            repository.joinLeague(inviteCode.trim())
                .onSuccess {
                    _events.emit("Você entrou na liga com sucesso!")
                }
                .onFailure {
                    _events.emit("Erro: ${it.message}")
                }
        }
    }
}
