package com.bolao.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bolao.presentation.TestTags
import com.bolao.presentation.leagues.LeaguesScreen
import com.bolao.presentation.theme.BolaoTheme
import com.bolao.testing.FakeLeagueRepository
import com.bolao.testing.TestData
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tela de Ligas: carregamento, erro, criar liga e entrar em liga por código.
 * Usa o LeaguesViewModel real (via Koin) sobre um [FakeLeagueRepository].
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [UiTestConfig.SDK], qualifiers = UiTestConfig.PHONE_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LeaguesScreenUiTest {

    private val leagueRepository = FakeLeagueRepository(
        initialLeagues = listOf(TestData.leagueAmigos),
        joinableLeagues = mapOf(TestData.leagueCopa.inviteCode to TestData.leagueCopa),
    )

    @get:Rule(order = 0)
    val koinRule = KoinTestRule(fakeRepositoriesModule(leagues = leagueRepository))

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    private val navigatedToLeagueIds = mutableListOf<String>()

    private fun setScreen() {
        composeRule.setContent {
            BolaoTheme {
                LeaguesScreen(onLeagueClick = { navigatedToLeagueIds += it })
            }
        }
    }

    @Test
    fun showsLoadingUntilDataArrives_thenShowsLeagues() {
        val gate = CompletableDeferred<Unit>()
        leagueRepository.loadGate = gate

        setScreen()

        // O repositório está "pendurado" no gate: a tela tem que mostrar o loading.
        composeRule.onNodeWithTag(TestTags.LEAGUES_LOADING).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.leagueCard(TestData.leagueAmigos.id)).assertDoesNotExist()

        // Libera a resposta. O dado chega por Flow/coroutine, então esperamos o nó aparecer.
        gate.complete(Unit)
        composeRule.waitForTag(TestTags.leagueCard(TestData.leagueAmigos.id))

        composeRule.onNodeWithTag(TestTags.leagueCard(TestData.leagueAmigos.id)).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.LEAGUES_LOADING).assertDoesNotExist()
    }

    @Test
    fun showsErrorState_whenLoadingLeaguesFails() {
        leagueRepository.loadError = IllegalStateException("Sem conexão com o servidor")

        setScreen()
        composeRule.waitForTag(TestTags.LEAGUES_ERROR)

        composeRule.onNodeWithTag(TestTags.LEAGUES_ERROR)
            .assertIsDisplayed()
            // A mensagem vem do fake (dado de teste), não de um texto fixo da UI.
            .assertTextEquals("Sem conexão com o servidor")
        composeRule.onNodeWithTag(TestTags.LEAGUES_LOADING).assertDoesNotExist()
    }

    @Test
    fun createLeague_sendsNameAndNickname_andNavigatesToNewLeague() {
        setScreen()
        composeRule.waitForTag(TestTags.leagueCard(TestData.leagueAmigos.id))

        composeRule.onNodeWithTag(TestTags.LEAGUES_CREATE_BUTTON).performClick()
        composeRule.onNodeWithTag(TestTags.CREATE_LEAGUE_NAME_FIELD).performTextInput("Liga do Trabalho")
        composeRule.onNodeWithTag(TestTags.CREATE_LEAGUE_NICKNAME_FIELD).performTextInput("Ana")
        composeRule.onNodeWithTag(TestTags.CREATE_LEAGUE_CONFIRM).performClick()

        // A navegação sai por um SharedFlow de eventos coletado num LaunchedEffect.
        composeRule.waitUntil(UiTestConfig.ASYNC_TIMEOUT_MS) { navigatedToLeagueIds.isNotEmpty() }

        assertEquals(listOf("Liga do Trabalho" to "Ana"), leagueRepository.createdLeagues)
        assertEquals(listOf("league_created_1"), navigatedToLeagueIds)
        composeRule.onNodeWithTag(TestTags.CREATE_LEAGUE_CONFIRM).assertDoesNotExist()
    }

    @Test
    fun joinLeagueByInviteCode_trimsInput_andShowsJoinedLeague() {
        setScreen()
        composeRule.waitForTag(TestTags.leagueCard(TestData.leagueAmigos.id))

        composeRule.onNodeWithTag(TestTags.LEAGUES_JOIN_BUTTON).performClick()
        // Espaços em volta simulam colar o código do WhatsApp.
        composeRule.onNodeWithTag(TestTags.JOIN_LEAGUE_CODE_FIELD).performTextInput("  ${TestData.leagueCopa.inviteCode} ")
        composeRule.onNodeWithTag(TestTags.JOIN_LEAGUE_NICKNAME_FIELD).performTextInput("Ana")
        composeRule.onNodeWithTag(TestTags.JOIN_LEAGUE_CONFIRM).performClick()

        composeRule.waitForTag(TestTags.leagueCard(TestData.leagueCopa.id))

        assertEquals(listOf(TestData.leagueCopa.inviteCode to "Ana"), leagueRepository.joinRequests)
        composeRule.onNodeWithTag(TestTags.leagueCard(TestData.leagueCopa.id)).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.leagueCard(TestData.leagueAmigos.id)).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.JOIN_LEAGUE_CONFIRM).assertDoesNotExist()
    }
}
