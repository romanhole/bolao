package com.bolao.presentation.matchlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import org.koin.compose.viewmodel.koinViewModel

/**
 * Tela de listagem dos palpites.
 * Agora é puramente um conteúdo interno, sem Scaffold nem TopAppBar.
 */
@Composable
fun MatchListScreen(
    modifier: Modifier = Modifier,
    viewModel: MatchListViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    // Observa erros de save e exibe como Snackbar
    val successState = uiState as? MatchListUiState.Success
    LaunchedEffect(successState) {
        successState?.items
            ?.firstOrNull { it.saveError != null }
            ?.let { item ->
                snackbarHost.showSnackbar(item.saveError ?: "Erro ao salvar")
                viewModel.clearSaveError(item.match.id)
            }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (val state = uiState) {
            is MatchListUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color    = MaterialTheme.colorScheme.primary,
                )
            }

            is MatchListUiState.Error -> {
                ErrorContent(
                    message  = state.message,
                    onRetry  = { /* TODO */ },
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            is MatchListUiState.Success -> {
                val listState = rememberLazyListState()
                
                // Encontra o índice da primeira partida não finalizada na lista atual
                val firstAvailableIndex = remember(state.items) {
                    state.items.indexOfFirst { 
                        it.match.status is com.bolao.domain.model.GameStatus.Scheduled || 
                        it.match.status is com.bolao.domain.model.GameStatus.Live 
                    }.takeIf { it >= 0 }
                }

                // Auto-scroll sempre que a aba do mês mudar
                LaunchedEffect(state.selectedRound) {
                    firstAvailableIndex?.let { index ->
                        listState.animateScrollToItem(index)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (state.availableRounds.isNotEmpty()) {
                            androidx.compose.material3.ScrollableTabRow(
                                selectedTabIndex = state.availableRounds.indexOf(state.selectedRound).coerceAtLeast(0),
                                edgePadding = 16.dp,
                                containerColor = MaterialTheme.colorScheme.background,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            ) {
                                state.availableRounds.forEach { round ->
                                    androidx.compose.material3.Tab(
                                        selected = state.selectedRound == round,
                                        onClick = { viewModel.selectRound(round) },
                                        text = { Text(round, style = MaterialTheme.typography.titleSmall) }
                                    )
                                }
                            }
                        }
                        
                        MatchListContent(
                            items           = state.items,
                            onUpdateHome    = viewModel::updateHomeGoals,
                            onUpdateAway    = viewModel::updateAwayGoals,
                            onSave          = viewModel::savePrediction,
                            modifier        = Modifier.fillMaxSize().weight(1f),
                            listState       = listState,
                            contentPadding  = PaddingValues(top = 16.dp, bottom = 100.dp),
                        )
                    }
                }
            }
        }

        // SnackbarHost exibido sobre o conteúdo
        SnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
        )
    }
}

@Composable
private fun MatchListContent(
    items: List<MatchPredictionItem>,
    onUpdateHome: (String, Int) -> Unit,
    onUpdateAway: (String, Int) -> Unit,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyColumn(
        state               = listState,
        modifier            = modifier,
        contentPadding      = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(
            items = items,
            key   = { it.match.id },
        ) { item ->
            MatchPredictionCard(
                item                = item,
                onHomeGoalIncrement = { onUpdateHome(item.match.id, 1) },
                onHomeGoalDecrement = { onUpdateHome(item.match.id, -1) },
                onAwayGoalIncrement = { onUpdateAway(item.match.id, 1) },
                onAwayGoalDecrement = { onUpdateAway(item.match.id, -1) },
                onSave              = { onSave(item.match.id) },
                modifier     = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    // Anima o aparecimento dos cards
                    .animateItem(),
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier            = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text      = message,
            style     = MaterialTheme.typography.bodyLarge,
            color     = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Tentar novamente")
        }
    }
}
