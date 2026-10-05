package com.bolao.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bolao.presentation.TestTags
import com.bolao.presentation.leagues.LeagueDetailScreen
import com.bolao.presentation.theme.BolaoTheme
import com.bolao.testing.FakeLeaderboardRepository
import com.bolao.testing.FakeLeagueRepository
import com.bolao.testing.TestData
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Classificação da liga: ordena por pontos e desempata por placares exatos.
 * Usa o LeagueDetailViewModel real (via Koin) sobre fakes de liga e ranking.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [UiTestConfig.SDK], qualifiers = UiTestConfig.PHONE_QUALIFIERS)
class LeagueDetailScreenUiTest {

    private val league = TestData.leagueAmigos

    @get:Rule(order = 0)
    val koinRule = KoinTestRule(
        fakeRepositoriesModule(
            leagues = FakeLeagueRepository(initialLeagues = listOf(league)),
            leaderboard = FakeLeaderboardRepository(mapOf(league.id to TestData.unorderedLeaderboard)),
        )
    )

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Test
    fun ranking_isSortedByPoints_thenByExactMatches() {
        composeRule.setContent { BolaoTheme { LeagueDetailScreen(leagueId = league.id, onBack = {}) } }
        composeRule.waitForTag(TestTags.rankingRow("user_carla"))

        composeRule.onNodeWithTag(TestTags.LEAGUE_DETAIL_TITLE).assertTextEquals(league.name)

        // Ordena pela posição vertical na tela (o que o usuário vê), e não pela ordem
        // da árvore de semântica, que não é garantida em listas lazy.
        val userIdsTopToBottom = composeRule
            .onAllNodes(hasTestTagStartingWith(TestTags.RANKING_ROW_PREFIX))
            .fetchSemanticsNodes()
            .sortedBy { it.positionInRoot.y }
            .map { it.config[SemanticsProperties.TestTag].removePrefix(TestTags.RANKING_ROW_PREFIX) }

        // Carla e Bruno empatam em 15 pontos; Carla tem mais placares exatos (4 x 2).
        assertEquals(listOf("user_carla", "user_bruno", "user_ana"), userIdsTopToBottom)
    }
}
