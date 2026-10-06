package com.bolao.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bolao.presentation.TestTags
import com.bolao.presentation.matchlist.MatchListScreen
import com.bolao.presentation.theme.BolaoTheme
import com.bolao.testing.FakeMatchRepository
import com.bolao.testing.FakePredictionRepository
import com.bolao.testing.TestData
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tela de Palpites: registrar um palpite numa partida aberta.
 * Usa o MatchListViewModel real (via Koin) sobre fakes de partidas, palpites e auth.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [UiTestConfig.SDK], qualifiers = UiTestConfig.PHONE_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MatchListScreenUiTest {

    private val match = TestData.scheduledMatch()
    private val predictionRepository = FakePredictionRepository()

    @get:Rule(order = 0)
    val koinRule = KoinTestRule(
        fakeRepositoriesModule(
            matches = FakeMatchRepository(listOf(match)),
            predictions = predictionRepository,
        )
    )

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Test
    fun registerPrediction_savesChosenScore_andDisablesSaveButton() {
        composeRule.setContent { BolaoTheme { MatchListScreen() } }
        composeRule.waitForTag(TestTags.saveButton(match.id))

        val home = TestTags.SIDE_HOME
        val away = TestTags.SIDE_AWAY
        composeRule.onNodeWithTag(TestTags.goalValue(match.id, home)).assertTextEquals("0")
        composeRule.onNodeWithTag(TestTags.goalValue(match.id, away)).assertTextEquals("0")

        // Brasil 2 x 1 Argentina. Cada clique anima o número (AnimatedContent); as
        // asserções seguintes esperam a UI ficar ociosa, então a animação já terminou.
        composeRule.onNodeWithTag(TestTags.goalIncrement(match.id, home)).performClick()
        composeRule.onNodeWithTag(TestTags.goalIncrement(match.id, home)).performClick()
        composeRule.onNodeWithTag(TestTags.goalIncrement(match.id, away)).performClick()

        composeRule.onNodeWithTag(TestTags.goalValue(match.id, home)).assertTextEquals("2")
        composeRule.onNodeWithTag(TestTags.goalValue(match.id, away)).assertTextEquals("1")

        composeRule.onNodeWithTag(TestTags.saveButton(match.id)).assertIsEnabled().performClick()

        // O save roda numa coroutine do ViewModel; esperamos o fake receber a chamada.
        composeRule.waitUntil(UiTestConfig.ASYNC_TIMEOUT_MS) { predictionRepository.savedPredictions.isNotEmpty() }

        val saved = predictionRepository.savedPredictions.single()
        assertEquals(match.id, saved.matchId)
        assertEquals(TestData.currentUser.userId, saved.userId)
        assertEquals(2, saved.predictedHome)
        assertEquals(1, saved.predictedAway)

        // Sem alterações pendentes, o botão fica desabilitado e o placar salvo continua na tela.
        composeRule.onNodeWithTag(TestTags.saveButton(match.id)).assertIsNotEnabled()
        composeRule.onNodeWithTag(TestTags.goalValue(match.id, home)).assertTextEquals("2")
        composeRule.onNodeWithTag(TestTags.goalValue(match.id, away)).assertTextEquals("1")
    }
}
