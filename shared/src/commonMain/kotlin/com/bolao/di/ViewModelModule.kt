package com.bolao.di

import com.bolao.data.repository.AuthRepositoryImpl
import com.bolao.data.repository.MatchRepositoryImpl
import com.bolao.data.repository.PredictionRepositoryImpl
import com.bolao.domain.repository.AuthRepository
import com.bolao.domain.repository.MatchRepository
import com.bolao.domain.repository.PredictionRepository
import com.bolao.presentation.auth.AuthViewModel
import com.bolao.presentation.matchlist.MatchListViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Módulo Koin de repositórios — implementações reais com Supabase.
 *
 * ## get() — injeção automática do SupabaseClient
 * O [SupabaseClient] registrado em [networkModule] é injetado automaticamente
 * em todos os repositórios via `get()`.
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
    single<AuthRepository>       { AuthRepositoryImpl(get()) }
}

/**
 * Módulo Koin de ViewModels.
 * [viewModelOf] registra o ViewModel com ciclo de vida KMP-compatible.
 */
val viewModelModule = module {
    viewModelOf(::MatchListViewModel)
    viewModelOf(::AuthViewModel)
}
