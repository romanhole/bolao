package com.bolao.presentation.leagues

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bolao.domain.model.League
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LeaguesScreen(
    modifier: Modifier = Modifier,
    onLeagueClick: (String) -> Unit,
    viewModel: LeaguesViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LeagueEvent.NavigateToLeague -> {
                    showCreateDialog = false
                    showJoinDialog = false
                    onLeagueClick(event.leagueId)
                }
                is LeagueEvent.ShowMessage -> {
                    showCreateDialog = false
                    showJoinDialog = false
                    snackbarHost.showSnackbar(event.message)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header: Botões de ação
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ElevatedButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Criar Liga", modifier = Modifier.padding(start = 8.dp))
                }
                
                ElevatedButton(
                    onClick = { showJoinDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = null)
                    Text("Entrar", modifier = Modifier.padding(start = 8.dp))
                }
            }

            // Conteúdo
            when (val state = uiState) {
                is LeaguesUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is LeaguesUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is LeaguesUiState.Success -> {
                    if (state.leagues.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "Você não participa de nenhuma liga privada. Crie uma ou entre com um código de convite!",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.leagues) { league ->
                                LeagueCard(
                                    league = league,
                                    onClick = { onLeagueClick(league.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
        )
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var nickname by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Criar Nova Liga") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome da Liga") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = { Text("Seu Apelido nesta Liga") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showCreateDialog = false
                    viewModel.createLeague(name, nickname)
                }) {
                    Text("Criar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showJoinDialog) {
        var code by remember { mutableStateOf("") }
        var nickname by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("Entrar em uma Liga") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Código de Convite") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = { Text("Seu Apelido nesta Liga") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showJoinDialog = false
                    viewModel.joinLeague(code, nickname)
                }) {
                    Text("Entrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun LeagueCard(
    league: League,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = league.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.padding(4.dp))
            Text(
                text = "Código de convite: ${league.inviteCode}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
