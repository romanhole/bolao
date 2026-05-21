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
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.rounded.Logout
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
import com.bolao.presentation.leaderboard.LeaderboardScreen
import com.bolao.presentation.leagues.LeagueDetailScreen
import com.bolao.presentation.leagues.LeaguesScreen
import com.bolao.presentation.matchlist.MatchListScreen
import com.bolao.presentation.theme.BolaoTheme
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

enum class AppTab(val title: String, val icon: ImageVector) {
    PREDICTIONS("Palpites", Icons.Default.SportsSoccer),
    LEAGUES("Ligas", Icons.Default.Group),
    LEADERBOARD("Ranking", Icons.Default.Leaderboard)
}

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
                    LoginScreen()

                is AuthState.Authenticated ->
                    AuthenticatedApp()
            }
        }
    }
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
fun MainTabsScreen(
    authViewModel: AuthViewModel,
    onNavigateToLeague: (String) -> Unit
) {
    var currentTab by remember { mutableStateOf(AppTab.PREDICTIONS) }
    val scope = rememberCoroutineScope()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text       = currentTab.title,
                            style      = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                        )
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
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                            )
                        },
                        label = { Text(tab.title) },
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "TabNavigation"
            ) { tab ->
                when (tab) {
                    AppTab.PREDICTIONS -> MatchListScreen()
                    AppTab.LEAGUES     -> LeaguesScreen(onLeagueClick = onNavigateToLeague)
                    AppTab.LEADERBOARD -> LeaderboardScreen()
                }
            }
        }
    }
}
