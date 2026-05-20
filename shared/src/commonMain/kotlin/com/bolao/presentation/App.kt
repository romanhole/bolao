package com.bolao.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bolao.domain.repository.AuthRepository
import com.bolao.domain.repository.AuthState
import com.bolao.presentation.auth.LoginScreen
import com.bolao.presentation.matchlist.MatchListScreen
import com.bolao.presentation.theme.BolaoTheme
import org.koin.compose.koinInject

/**
 * Composable raiz da aplicação — gerencia o roteamento entre telas com base
 * no estado de autenticação.
 *
 * ## Fluxo de navegação
 * ```
 * AuthState.Loading         → spinner centralizado (sessão sendo restaurada)
 * AuthState.NotAuthenticated → LoginScreen
 * AuthState.Authenticated   → MatchListScreen
 * ```
 *
 * ## Por que aqui e não na Activity?
 * Como o estado de auth é reativo (Flow), [App] observa e recompõe automaticamente.
 * Sem `startActivity`, sem flags de intent — simplesmente o Compose faz o trabalho.
 *
 * ## BolaoTheme
 * O tema é aplicado uma única vez aqui (não em cada tela filha) para garantir
 * consistência e evitar duplicação de MaterialTheme no composition tree.
 */
@Composable
fun App(
    authRepository: AuthRepository = koinInject(),
) {
    BolaoTheme {
        val authState by authRepository.authState.collectAsState(initial = AuthState.Loading)

        AnimatedContent(
            targetState   = authState,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label         = "AuthNavigation",
        ) { state ->
            when (state) {

                // Sessão sendo restaurada do armazenamento — mostra spinner discreto
                is AuthState.Loading ->
                    Box(
                        modifier         = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color    = MaterialTheme.colorScheme.primary,
                        )
                    }

                // Não autenticado — tela de login/cadastro
                is AuthState.NotAuthenticated ->
                    LoginScreen()

                // Autenticado — tela principal de palpites
                is AuthState.Authenticated ->
                    MatchListScreen()
            }
        }
    }
}
