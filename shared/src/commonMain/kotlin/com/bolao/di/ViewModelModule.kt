package com.bolao.di

import com.bolao.data.repository.MatchRepositoryImpl
import com.bolao.data.repository.PredictionRepositoryImpl
import com.bolao.domain.repository.MatchRepository
import com.bolao.domain.repository.PredictionRepository
import com.bolao.presentation.matchlist.MatchListViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Módulo Koin de repositórios — implementações reais com Supabase.
 *
 * ## Troca Fake → Real
 * A única mudança necessária foi aqui: os bindings agora apontam para
 * [MatchRepositoryImpl] e [PredictionRepositoryImpl] em vez das implementações Fake.
 *
 * O [MatchListViewModel] não mudou — ele depende das interfaces [MatchRepository]
 * e [PredictionRepository], nunca das implementações concretas. Isso é Clean Architecture.
 *
 * ## get() — injeção automática do SupabaseClient
 * `get()` resolve o [SupabaseClient] registrado em [networkModule].
 * O Koin garante que é o mesmo singleton para todos os repositórios.
 *
 * ## Rollback para desenvolvimento offline
 * Para voltar aos Fakes (ex: testar UI sem internet), basta trocar:
 * ```kotlin
 * single<MatchRepository> { FakeMatchRepository() }
 * ```
 */
val repositoryModule = module {
    single<MatchRepository>      { MatchRepositoryImpl(get()) }
    single<PredictionRepository> { PredictionRepositoryImpl(get()) }
}

/**
 * Módulo Koin de ViewModels.
 * [viewModelOf] registra o ViewModel com ciclo de vida KMP-compatible.
 */
val viewModelModule = module {
    viewModelOf(::MatchListViewModel)
}
