package com.bolao.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.bolao.presentation.theme.BolaoGold
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordSheet(
    onDismiss: () -> Unit,
    authViewModel: AuthViewModel = koinViewModel(),
) {
    val state by authViewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // Fecha automaticamente após 2s em caso de sucesso
    LaunchedEffect(state.changePasswordSuccess) {
        if (state.changePasswordSuccess) {
            delay(2000)
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Título
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = BolaoGold,
                )
                Text(
                    text = "Trocar Senha",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Tela de sucesso
            AnimatedVisibility(
                visible = state.changePasswordSuccess,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = BolaoGold,
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        text = "Senha alterada com sucesso!",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = BolaoGold,
                    )
                }
            }

            // Formulário (oculto no sucesso)
            AnimatedVisibility(
                visible = !state.changePasswordSuccess,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Campo nova senha
                    OutlinedTextField(
                        value = state.newPassword,
                        onValueChange = authViewModel::onNewPasswordChange,
                        label = { Text("Nova senha") },
                        singleLine = true,
                        visualTransformation = if (state.isNewPasswordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = authViewModel::toggleNewPasswordVisibility) {
                                Icon(
                                    imageVector = if (state.isNewPasswordVisible)
                                        Icons.Default.VisibilityOff
                                    else
                                        Icons.Default.Visibility,
                                    contentDescription = if (state.isNewPasswordVisible) "Ocultar senha" else "Mostrar senha",
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.changePasswordLoading,
                    )

                    // Campo confirmar nova senha
                    OutlinedTextField(
                        value = state.confirmNewPassword,
                        onValueChange = authViewModel::onConfirmNewPasswordChange,
                        label = { Text("Confirmar nova senha") },
                        singleLine = true,
                        visualTransformation = if (state.isConfirmNewPasswordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = authViewModel::toggleConfirmNewPasswordVisibility) {
                                Icon(
                                    imageVector = if (state.isConfirmNewPasswordVisible)
                                        Icons.Default.VisibilityOff
                                    else
                                        Icons.Default.Visibility,
                                    contentDescription = if (state.isConfirmNewPasswordVisible) "Ocultar senha" else "Mostrar senha",
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.changePasswordLoading,
                    )

                    // Regra de senha
                    Text(
                        text = "• Mínimo 6 caracteres",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // Mensagem de erro
                    if (state.changePasswordError != null) {
                        Text(
                            text = state.changePasswordError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Botão salvar
                    Button(
                        onClick = authViewModel::submitChangePassword,
                        enabled = !state.changePasswordLoading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.changePasswordLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text("Salvar Senha", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
