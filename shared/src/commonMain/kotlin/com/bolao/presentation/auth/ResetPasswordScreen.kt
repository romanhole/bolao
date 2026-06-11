package com.bolao.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bolao.presentation.theme.BolaoGold
import com.bolao.presentation.theme.BolaoGreen
import com.bolao.presentation.theme.BolaoTheme

@Composable
fun ResetPasswordScreen(
    viewModel: AuthViewModel,
) {
    BolaoTheme {
        val uiState by viewModel.uiState.collectAsState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(48.dp))

                // ── Logo ─────────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(BolaoGreen, BolaoGold.copy(alpha = 0.8f))
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = "B",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            color      = Color.White,
                        ),
                    )
                }

                // ── Título ───────────────────────────────────────────────────
                Text(
                    text       = "Nova Senha",
                    style      = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color      = MaterialTheme.colorScheme.primary,
                )

                Text(
                    text      = "Defina sua nova senha de acesso",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(4.dp))

                // ── Card do formulário ────────────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {

                        // ── Campo Nova Senha ───────────────────────────────────────
                        OutlinedTextField(
                            value         = uiState.newPassword,
                            onValueChange = viewModel::onNewPasswordChange,
                            label         = { Text("Nova Senha") },
                            leadingIcon   = {
                                Icon(
                                    imageVector        = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint               = MaterialTheme.colorScheme.primary,
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = viewModel::toggleNewPasswordVisibility) {
                                    Icon(
                                        imageVector = if (uiState.isNewPasswordVisible)
                                            Icons.Rounded.VisibilityOff
                                        else
                                            Icons.Rounded.Visibility,
                                        contentDescription = if (uiState.isNewPasswordVisible)
                                            "Ocultar senha"
                                        else
                                            "Mostrar senha",
                                    )
                                }
                            },
                            visualTransformation = if (uiState.isNewPasswordVisible)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction    = ImeAction.Next,
                            ),
                            singleLine = true,
                            modifier   = Modifier.fillMaxWidth(),
                            shape      = RoundedCornerShape(12.dp),
                            colors     = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            ),
                        )

                        // ── Campo Confirmar Nova Senha ─────────────────────────────
                        OutlinedTextField(
                            value         = uiState.confirmNewPassword,
                            onValueChange = viewModel::onConfirmNewPasswordChange,
                            label         = { Text("Confirmar Nova Senha") },
                            leadingIcon   = {
                                Icon(
                                    imageVector        = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint               = MaterialTheme.colorScheme.primary,
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = viewModel::toggleConfirmNewPasswordVisibility) {
                                    Icon(
                                        imageVector = if (uiState.isConfirmNewPasswordVisible)
                                            Icons.Rounded.VisibilityOff
                                        else
                                            Icons.Rounded.Visibility,
                                        contentDescription = if (uiState.isConfirmNewPasswordVisible)
                                            "Ocultar senha"
                                        else
                                            "Mostrar senha",
                                    )
                                }
                            },
                            visualTransformation = if (uiState.isConfirmNewPasswordVisible)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction    = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { viewModel.submitNewPassword() }
                            ),
                            singleLine = true,
                            modifier   = Modifier.fillMaxWidth(),
                            shape      = RoundedCornerShape(12.dp),
                            colors     = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            ),
                        )

                        // ── Mensagem de erro ──────────────────────────────────
                        AnimatedVisibility(
                            visible = uiState.resetPasswordError != null,
                            enter   = fadeIn(),
                            exit    = fadeOut(),
                        ) {
                            Text(
                                text     = uiState.resetPasswordError.orEmpty(),
                                style    = MaterialTheme.typography.bodySmall,
                                color    = MaterialTheme.colorScheme.error,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        // ── Botão de salvar ───────────────────────────────────
                        Button(
                            onClick  = viewModel::submitNewPassword,
                            enabled  = !uiState.resetPasswordLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape  = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BolaoGreen,
                                contentColor   = Color.Black,
                            ),
                        ) {
                            if (uiState.resetPasswordLoading) {
                                CircularProgressIndicator(
                                    modifier    = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp,
                                    color       = Color.Black,
                                )
                            } else {
                                Text(
                                    text       = "Salvar nova senha",
                                    style      = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                // ── Cancelar ───────────────────────────────────
                TextButton(onClick = viewModel::cancelResetPassword) {
                    Text(
                        text  = "Cancelar e Sair",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Spacer(Modifier.height(48.dp))
            }
        }
    }
}
