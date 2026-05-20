package com.bolao.presentation.matchlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bolao.presentation.auth.AuthViewModel
import com.bolao.presentation.theme.BolaoTheme
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * Tela principal do Bolão — lista de partidas com palpites.
 *
 * ## Arquitetura
 * - Obtém o [MatchListViewModel] via Koin ([koinViewModel])
 * - Observa [MatchListUiState] com [collectAsState]
 * - Delega completamente os callbacks de UI para o ViewModel
 *
 * ## Estados de UI
 * - [MatchListUiState.Loading]  → CircularProgressIndicator centralizado
 * - [MatchListUiState.Success]  → [MatchListContent] com LazyColumn
 * - [MatchListUiState.Error]    → [ErrorContent] com mensagem e botão de retry
 *
 * ## Integração
 * Chame esta função diretamente no `setContent {}` do `MainActivity` (Android)
 * e no `ComposeUIViewController` do iOS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchListScreen(
    viewModel: MatchListViewModel = koinViewModel(),
    authViewModel: AuthViewModel  = koinViewModel(),
) {
    BolaoTheme {
        val uiState      by viewModel.uiState.collectAsState()
        val snackbarHost = remember { SnackbarHostState() }
        val scope        = rememberCoroutineScope()

        // Observa erros de save e exibe como Snackbar (UX não-intrusiva)
        val successState = uiState as? MatchListUiState.Success
        LaunchedEffect(successState) {
            successState?.items
                ?.firstOrNull { it.saveError != null }
                ?.let { item ->
                    snackbarHost.showSnackbar(item.saveError ?: "Erro ao salvar")
                    viewModel.clearSaveError(item.match.id)
                }
        }

        // Scroll behavior para colapsar a TopAppBar ao rolar
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState()
        )

        Scaffold(
            modifier    = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            snackbarHost = { SnackbarHost(snackbarHost) },
            topBar      = {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text(
                                text       = "Meus Palpites",
                                style      = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                            )
                            AnimatedVisibility(visible = successState != null) {
                                Text(
                                    text  = "${successState?.items?.size ?: 0} partidas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor         = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    scrollBehavior = scrollBehavior,
                    actions = {
                        IconButton(
                            onClick = { scope.launch { authViewModel.logout() } }
                        ) {
                            Icon(
                                imageVector        = Icons.Rounded.Logout,
                                contentDescription = "Sair",
                                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { paddingValues ->

            when (val state = uiState) {
                is MatchListUiState.Loading ->
                    LoadingContent(modifier = Modifier.padding(paddingValues))

                is MatchListUiState.Error ->
                    ErrorContent(
                        message  = state.message,
                        onRetry  = { /* TODO: expose retry in ViewModel */ },
                        modifier = Modifier.padding(paddingValues),
                    )

                is MatchListUiState.Success ->
                    MatchListContent(
                        items                = state.items,
                        onHomeGoalIncrement  = { matchId -> viewModel.updateHomeGoals(matchId, +1) },
                        onHomeGoalDecrement  = { matchId -> viewModel.updateHomeGoals(matchId, -1) },
                        onAwayGoalIncrement  = { matchId -> viewModel.updateAwayGoals(matchId, +1) },
                        onAwayGoalDecrement  = { matchId -> viewModel.updateAwayGoals(matchId, -1) },
                        onSave               = { matchId -> viewModel.savePrediction(matchId) },
                        modifier             = Modifier.padding(paddingValues),
                    )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Estados de conteúdo
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Lista principal com LazyColumn.
 *
 * - `key = { it.match.id }` garante recomposição eficiente (sem re-render total)
 * - `animateItem()` adiciona animação suave ao inserir/remover partidas
 * - Espaçamento vertical de 12dp entre os cards
 */
@Composable
private fun MatchListContent(
    items: List<MatchPredictionItem>,
    onHomeGoalIncrement: (String) -> Unit,
    onHomeGoalDecrement: (String) -> Unit,
    onAwayGoalIncrement: (String) -> Unit,
    onAwayGoalDecrement: (String) -> Unit,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        EmptyContent(modifier = modifier)
        return
    }

    LazyColumn(
        modifier        = modifier.fillMaxSize(),
        contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = items,
            key   = { it.match.id },
        ) { item ->
            AnimatedVisibility(
                visible      = true,
                enter        = slideInVertically(tween(300)) { it / 2 } + fadeIn(tween(300)),
                modifier     = Modifier.animateItem(),
            ) {
                MatchPredictionCard(
                    item                 = item,
                    onHomeGoalIncrement  = { onHomeGoalIncrement(item.match.id) },
                    onHomeGoalDecrement  = { onHomeGoalDecrement(item.match.id) },
                    onAwayGoalIncrement  = { onAwayGoalIncrement(item.match.id) },
                    onAwayGoalDecrement  = { onAwayGoalDecrement(item.match.id) },
                    onSave               = { onSave(item.match.id) },
                )
            }
        }
    }
}

// ── Loading ───────────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier         = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
    }
}

// ── Error ─────────────────────────────────────────────────────────────────────

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier         = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(24.dp),
        ) {
            Text(
                text      = "⚠\uFE0F Ops!",
                style     = MaterialTheme.typography.headlineSmall,
                fontWeight= FontWeight.Bold,
                color     = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text      = message,
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onRetry) {
                Text("Tentar novamente")
            }
        }
    }
}

// ── Empty ─────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Box(
        modifier         = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(24.dp),
        ) {
            Icon(
                imageVector      = Icons.Rounded.Check,
                contentDescription = null,
                tint             = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier         = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text      = "Nenhuma partida disponível",
                style     = MaterialTheme.typography.titleMedium,
                color     = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text      = "As partidas aparecerão aqui quando forem cadastradas.",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(top = 4.dp),
            )
        }
    }
}
