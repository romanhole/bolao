package com.bolao.presentation.leagues

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.Info
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bolao.domain.model.League
import com.bolao.domain.model.LeaderboardItem
import com.bolao.presentation.BackHandlerWrapper
import com.bolao.presentation.theme.BolaoGold
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueDetailScreen(
    leagueId: String,
    onBack: () -> Unit,
    viewModel: LeagueDetailViewModel = koinViewModel()
) {
    // Intercepta o botão físico de Voltar no Android
    BackHandlerWrapper(enabled = true, onBack = onBack)

    LaunchedEffect(leagueId) {
        viewModel.loadLeagueDetail(leagueId)
    }

    val uiState by viewModel.uiState.collectAsState()
    var showRulesBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = (uiState as? LeagueDetailUiState.Success)?.league?.name ?: "Liga"
                    Text(title, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showRulesBottomSheet = true }) {
                        Icon(Icons.Rounded.Info, contentDescription = "Regras de Pontuação")
                    }
                    val league = (uiState as? LeagueDetailUiState.Success)?.league
                    if (league != null) {
                        IconButton(onClick = { shareLeagueInvite(league) }) {
                            Icon(Icons.Default.Share, contentDescription = "Compartilhar convite")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is LeagueDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is LeagueDetailUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        Button(onClick = { viewModel.loadLeagueDetail(leagueId) }, modifier = Modifier.padding(top = 16.dp)) {
                            Text("Tentar novamente")
                        }
                    }
                }
                is LeagueDetailUiState.Success -> {
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = viewModel::refresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (state.ranking.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Nenhum membro com palpites nesta liga ainda.",
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                itemsIndexed(state.ranking) { index, item ->
                                    LeagueRankingCard(
                                        position = index + 1,
                                        item = item,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showRulesBottomSheet) {
                com.bolao.presentation.matchlist.RulesBottomSheet(
                    onDismissRequest = { showRulesBottomSheet = false }
                )
            }
        }
    }
}

@Composable
private fun LeagueRankingCard(
    position: Int,
    item: LeaderboardItem,
    modifier: Modifier = Modifier,
) {
    val isPodium = position <= 3

    val surfaceColor = when (position) {
        1 -> BolaoGold.copy(alpha = 0.15f)
        2 -> Color(0xFFC0C0C0).copy(alpha = 0.15f)
        3 -> Color(0xFFCD7F32).copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.surface
    }

    val positionColor = when (position) {
        1 -> BolaoGold
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val initial = item.nickname.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val avatarColor = avatarColorFor(item.userId)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = surfaceColor,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${position}º",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = positionColor,
                modifier = Modifier.width(36.dp),
            )

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(avatarColor.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = avatarColor,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.nickname,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${item.exactMatches} placar exato • ${item.totalPredictionsMade} palpites",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = item.totalPoints.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = if (isPodium) positionColor else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "pts",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Gera uma cor consistente para o avatar baseada no userId. */
private fun avatarColorFor(userId: String): Color {
    val palette = listOf(
        Color(0xFF6C63FF), Color(0xFF00C896), Color(0xFFFF6B6B),
        Color(0xFFFFB347), Color(0xFF4FC3F7), Color(0xFFBA68C8),
    )
    return palette[userId.hashCode().and(0x7fffffff) % palette.size]
}

/** Compartilha o código de convite da liga via Intent nativo (expect/actual). */
expect fun shareLeagueInvite(league: League)
