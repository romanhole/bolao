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
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
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
import org.koin.compose.viewmodel.koinViewModel

/**
 * Tela de login e cadastro.
 *
 * ## Design
 * - Fundo escuro com gradiente radial suave
 * - Card central com efeito glassmorphism
 * - Campo de e-mail com ícone de e-mail
 * - Campo de senha com botão de mostrar/ocultar
 * - Animação de transição entre modo login ↔ cadastro
 * - Spinner e desabilita botão durante loading
 *
 * ## Navegação
 * Não há navegação explícita — o [App] composable raiz observa
 * [AuthRepository.authState] e redireciona para [MatchListScreen]
 * automaticamente após login/cadastro bem-sucedido.
 */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel = koinViewModel(),
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
                        text  = "⚽",
                        style = MaterialTheme.typography.headlineLarge,
                    )
                }

                // ── Título ───────────────────────────────────────────────────
                Text(
                    text       = "Bolão",
                    style      = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color      = MaterialTheme.colorScheme.primary,
                )

                Text(
                    text      = if (uiState.isLoginMode)
                        "Entre para fazer seus palpites"
                    else
                        "Crie sua conta gratuitamente",
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

                        // ── Campo E-mail ──────────────────────────────────────
                        OutlinedTextField(
                            value         = uiState.email,
                            onValueChange = viewModel::onEmailChange,
                            label         = { Text("E-mail") },
                            leadingIcon   = {
                                Icon(
                                    imageVector        = Icons.Rounded.Email,
                                    contentDescription = null,
                                    tint               = MaterialTheme.colorScheme.primary,
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
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



                        // ── Campo Senha ───────────────────────────────────────
                        OutlinedTextField(
                            value         = uiState.password,
                            onValueChange = viewModel::onPasswordChange,
                            label         = { Text("Senha") },
                            leadingIcon   = {
                                Icon(
                                    imageVector        = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint               = MaterialTheme.colorScheme.primary,
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = viewModel::togglePasswordVisibility) {
                                    Icon(
                                        imageVector = if (uiState.isPasswordVisible)
                                            Icons.Rounded.VisibilityOff
                                        else
                                            Icons.Rounded.Visibility,
                                        contentDescription = if (uiState.isPasswordVisible)
                                            "Ocultar senha"
                                        else
                                            "Mostrar senha",
                                    )
                                }
                            },
                            visualTransformation = if (uiState.isPasswordVisible)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction    = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { viewModel.submit() }
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
                            visible = uiState.error != null,
                            enter   = fadeIn(),
                            exit    = fadeOut(),
                        ) {
                            Text(
                                text     = uiState.error.orEmpty(),
                                style    = MaterialTheme.typography.bodySmall,
                                color    = MaterialTheme.colorScheme.error,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        // ── Botão de submit ───────────────────────────────────
                        Button(
                            onClick  = viewModel::submit,
                            enabled  = !uiState.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape  = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BolaoGreen,
                                contentColor   = Color.Black,
                            ),
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier    = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp,
                                    color       = Color.Black,
                                )
                            } else {
                                Text(
                                    text       = if (uiState.isLoginMode) "Entrar" else "Criar Conta",
                                    style      = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                // ── Toggle login ↔ cadastro ───────────────────────────────────
                TextButton(onClick = viewModel::toggleMode) {
                    Text(
                        text  = if (uiState.isLoginMode)
                            "Não tem conta? Criar agora"
                        else
                            "Já tem conta? Entrar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Spacer(Modifier.height(48.dp))
            }
        }
    }
}
