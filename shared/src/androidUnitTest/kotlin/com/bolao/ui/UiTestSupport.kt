package com.bolao.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.bolao.di.viewModelModule
import com.bolao.domain.repository.AuthRepository
import com.bolao.domain.repository.LeaderboardRepository
import com.bolao.domain.repository.LeagueRepository
import com.bolao.domain.repository.MatchRepository
import com.bolao.domain.repository.PredictionRepository
import com.bolao.testing.FakeAuthRepository
import com.bolao.testing.FakeLeaderboardRepository
import com.bolao.testing.FakeLeagueRepository
import com.bolao.testing.FakeMatchRepository
import com.bolao.testing.FakePredictionRepository
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Configuração Robolectric comum aos testes de UI.
 * - SDK 34: último que o Robolectric roda em JDK 17 (o do CI); o app compila com 36.
 * - Tela de celular comum (411x914dp), para os layouts "Compact" do app.
 */
object UiTestConfig {
    const val SDK = 34
    const val PHONE_QUALIFIERS = "w411dp-h914dp-xxhdpi"

    /** Tempo máximo de espera por dados assíncronos. Só é atingido se o teste for falhar. */
    const val ASYNC_TIMEOUT_MS = 5_000L
}

/**
 * Substitui SÓ a camada de dados por fakes. O `viewModelModule` é o mesmo de produção,
 * então a fiação Koin dos ViewModels também é exercitada pelos testes.
 *
 * O `networkModule` (SupabaseClient, HttpClient) não é carregado: se alguma tela tentar
 * resolver algo de rede, o Koin falha na hora com NoDefinitionFoundException, em vez de
 * o teste depender da internet em silêncio.
 */
fun fakeRepositoriesModule(
    auth: AuthRepository = FakeAuthRepository(),
    leagues: LeagueRepository = FakeLeagueRepository(),
    matches: MatchRepository = FakeMatchRepository(),
    predictions: PredictionRepository = FakePredictionRepository(),
    leaderboard: LeaderboardRepository = FakeLeaderboardRepository(),
): Module = module {
    single { auth }
    single { leagues }
    single { matches }
    single { predictions }
    single { leaderboard }
}

/**
 * Inicia um Koin limpo antes de cada teste e o encerra depois, para que nenhum
 * estado (singletons, fakes) vaze de um teste para outro.
 * Deve rodar antes da regra do Compose (`@get:Rule(order = 0)`).
 */
class KoinTestRule(private val dataModule: Module) : TestWatcher() {
    override fun starting(description: Description) {
        stopKoin()
        startKoin { modules(dataModule, viewModelModule) }
    }

    override fun finished(description: Description) {
        stopKoin()
    }
}

fun hasTestTagStartingWith(prefix: String) = SemanticsMatcher("TestTag começa com '$prefix'") { node ->
    node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
}

/**
 * Espera um nó com [tag] existir. Usado quando o dado chega por coroutine/Flow
 * (fora do que o Compose rastreia como "ocupado"). Faz polling com o relógio do
 * Compose e falha com timeout claro, em vez de usar Thread.sleep.
 */
fun ComposeContentTestRule.waitForTag(tag: String) {
    waitUntil(UiTestConfig.ASYNC_TIMEOUT_MS) {
        onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
    }
}
