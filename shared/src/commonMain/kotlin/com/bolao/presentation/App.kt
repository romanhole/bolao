package com.bolao.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bolao.domain.repository.AuthRepository
import com.bolao.domain.repository.AuthState
import com.bolao.presentation.auth.AuthViewModel
import com.bolao.presentation.auth.LoginScreen
import com.bolao.presentation.leagues.LeagueDetailScreen
import com.bolao.presentation.leagues.LeaguesScreen
import com.bolao.presentation.matchlist.MatchListScreen
import com.bolao.presentation.theme.BolaoTheme
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import io.kamel.core.config.KamelConfig
import io.kamel.core.config.takeFrom
import io.kamel.core.config.httpUrlFetcher
import io.kamel.image.config.LocalKamelConfig
import io.kamel.image.config.Default
import io.ktor.client.HttpClient
import com.bolao.presentation.auth.ResetPasswordScreen
import androidx.compose.runtime.CompositionLocalProvider

enum class AppTab(val title: String, val icon: ImageVector) {
    PREDICTIONS("Palpites", Icons.Default.Stars),
    LEAGUES("Ligas", Icons.Default.Group),
}

@Composable
fun App(
    appVersionCode: Int,
    authRepository: AuthRepository = koinInject(),
    authViewModel: AuthViewModel = koinViewModel(),
) {
    val settingsRepository: com.bolao.domain.repository.SettingsRepository = koinInject()
    var appSettings by remember { mutableStateOf<com.bolao.domain.model.AppSettings?>(null) }
    var dismissedSoftUpdate by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        coroutineScope.launch {
            settingsRepository.getSettings().onSuccess { settings ->
                appSettings = settings
            }
        }
    }

    val httpClient: HttpClient = koinInject()
    val kamelConfig = remember(httpClient) {
        KamelConfig {
            takeFrom(KamelConfig.Default)
            httpUrlFetcher(httpClient)
        }
    }

    CompositionLocalProvider(LocalKamelConfig provides kamelConfig) {
        BolaoTheme {
            val settings = appSettings
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

            if (settings != null && appVersionCode < settings.minVersionCode) {
                UpdateRequiredDialog(onUpdate = { uriHandler.openUri("https://play.google.com/store/apps/details?id=com.bolao.android") })
            } else {
                if (settings != null && appVersionCode < settings.latestVersionCode && !dismissedSoftUpdate) {
                    UpdateSuggestedDialog(
                        onDismiss = { dismissedSoftUpdate = true },
                        onUpdate = { uriHandler.openUri("https://play.google.com/store/apps/details?id=com.bolao.android") }
                    )
                }
                
                val authState by authRepository.authState.collectAsState(initial = AuthState.Loading)
            val uiState by authViewModel.uiState.collectAsState()

            val settingsManager = remember { com.russhwolf.settings.Settings() }
            var hasSeenTutorial by remember { androidx.compose.runtime.mutableStateOf(settingsManager.getBoolean("has_seen_tutorial", false)) }

            AnimatedContent(
                targetState   = authState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label         = "AuthNavigation",
            ) { state ->
                when (state) {
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

                    is AuthState.NotAuthenticated ->
                        LoginScreen(viewModel = authViewModel)

                    is AuthState.Authenticated -> {
                        if (uiState.isNewPasswordMode) {
                            ResetPasswordScreen(viewModel = authViewModel, uiState = uiState)
                        } else if (!hasSeenTutorial) {
                            com.bolao.presentation.onboarding.OnboardingScreen(
                                onFinish = { 
                                    settingsManager.putBoolean("has_seen_tutorial", true)
                                    hasSeenTutorial = true
                                }
                            )
                        } else {
                            AuthenticatedApp(authViewModel = authViewModel)
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
fun UpdateRequiredDialog(onUpdate: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { /* Não pode fechar */ },
        title = {
            Text("Atualização Necessária", fontWeight = FontWeight.Bold)
        },
        text = {
            Text("Sua versão do Bolão Campeão está muito antiga e deixou de ser suportada. Por favor, atualize o aplicativo na loja para continuar palpitando.")
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = onUpdate
            ) {
                Text("Atualizar Agora")
            }
        },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    )
}

@Composable
fun UpdateSuggestedDialog(onDismiss: () -> Unit, onUpdate: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Nova Versão Disponível", fontWeight = FontWeight.Bold)
        },
        text = {
            Text("Temos novidades fresquinhas no Bolão Campeão! Atualize agora para aproveitar as melhorias mais recentes.")
        },
        confirmButton = {
            androidx.compose.material3.Button(onClick = onUpdate) {
                Text("Atualizar")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Agora Não")
            }
        }
    )
}


sealed interface AuthRoute {
    data object MainTabs : AuthRoute
    data class LeagueDetail(val leagueId: String) : AuthRoute
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedApp(
    authViewModel: AuthViewModel = koinViewModel(),
) {
    var currentRoute by remember { mutableStateOf<AuthRoute>(AuthRoute.MainTabs) }

    AnimatedContent(
        targetState = currentRoute,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "AuthRouteNavigation"
    ) { route ->
        when (route) {
            is AuthRoute.MainTabs -> {
                MainTabsScreen(
                    authViewModel = authViewModel,
                    onNavigateToLeague = { currentRoute = AuthRoute.LeagueDetail(it) }
                )
            }
            is AuthRoute.LeagueDetail -> {
                LeagueDetailScreen(
                    leagueId = route.leagueId,
                    onBack = { currentRoute = AuthRoute.MainTabs }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabsScreen(authViewModel: AuthViewModel, onNavigateToLeague: (String) -> Unit) {
    var currentTab by remember { mutableStateOf(AppTab.PREDICTIONS) }
    var showRulesBottomSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val windowWidthClass = rememberWindowWidthClass()
    val isCompact = windowWidthClass == WindowWidthClass.Compact

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentTab.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                scrollBehavior = scrollBehavior,
                actions = {
                    if (currentTab == AppTab.PREDICTIONS) {
                        IconButton(onClick = { showRulesBottomSheet = true }) {
                            Icon(Icons.Rounded.Info, contentDescription = "Regras de Pontuação")
                        }
                    }
                    IconButton(onClick = { scope.launch { authViewModel.logout() } }) {
                        Icon(Icons.Rounded.Logout, contentDescription = "Sair")
                    }
                },
            )
        },
        bottomBar = {
            if (isCompact) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    AppTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title) },
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Row(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (!isCompact) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    AppTab.entries.forEach { tab ->
                        NavigationRailItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title) },
                        )
                    }
                }
            }
            
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "TabNavigation"
                ) { tab ->
                    when (tab) {
                        AppTab.PREDICTIONS -> MatchListScreen()
                        AppTab.LEAGUES -> LeaguesScreen(onLeagueClick = onNavigateToLeague)
                    }
                }

                if (showRulesBottomSheet) {
                    com.bolao.presentation.matchlist.RulesBottomSheet(onDismissRequest = { showRulesBottomSheet = false })
                }
            }
        }
    }
}
