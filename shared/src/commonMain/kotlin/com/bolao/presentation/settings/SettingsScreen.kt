package com.bolao.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bolao.presentation.BackHandlerWrapper
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandlerWrapper(enabled = true, onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Notificações",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            if (!viewModel.isPushSupported()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    PaddingValues(16.dp)
                    Text(
                        text = "As notificações push estão disponíveis apenas no aplicativo Android. Em breve teremos novidades para a versão web!",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                // Toggle de Notificações
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Receber alertas", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "Avisaremos você para não esquecer de palpitar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isNotificationsEnabled,
                        onCheckedChange = { viewModel.toggleNotifications(it, null) }
                    )
                }

                if (uiState.isNotificationsEnabled) {
                    Divider()

                    Text("Antecedência do alerta", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Quanto tempo antes da partida você quer ser avisado?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val options = listOf(1, 2, 3, 6, 12, 24)

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        options.forEach { hours ->
                            val label = if (hours == 1) "1 hora" else "$hours horas"
                            FilterChip(
                                selected = uiState.hoursBeforeMatch == hours,
                                onClick = { viewModel.updateHoursBeforeMatch(hours, null) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPermissionDialog() },
            title = { Text("Permissão Necessária") },
            text = {
                Text(
                    "Para receber alertas sobre os jogos, você precisa habilitar as notificações do Bolão nas configurações do seu celular."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.openSystemSettings() }) {
                    Text("Abrir Configurações")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPermissionDialog() }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
