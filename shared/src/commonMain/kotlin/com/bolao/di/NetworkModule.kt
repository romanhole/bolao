package com.bolao.di

import com.bolao.data.network.createHttpClient
import com.bolao.config.AppConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.dsl.module

/**
 * Módulo Koin de rede.
 *
 * ## SupabaseClient (singleton)
 * O cliente Supabase é compartilhado por todos os repositórios.
 * Internamente, o SDK gerencia um pool de conexões Ktor.
 *
 * ### Postgrest
 * Habilita queries REST via PostgREST (SELECT, INSERT, UPDATE, UPSERT, DELETE).
 *
 * ### Realtime
 * Habilita subscriptions via WebSocket para eventos Postgres Change.
 * **IMPORTANTE:** O SDK v3 não conecta automaticamente — chamamos [connect()] logo
 * após a criação do cliente em um escopo global com [SupervisorJob].
 * A conexão fica aberta enquanto o app estiver rodando; o SDK cuida de reconexão
 * automática em caso de queda. Não há necessidade de desconectar manualmente.
 *
 * ### Auth
 * Gerencia sessão do usuário (login, signup, logout).
 * Persiste o token automaticamente (SharedPreferences no Android / Keychain no iOS).
 * O `authState` flow em [AuthRepositoryImpl] é alimentado por `sessionStatus`.
 *
 * ## HttpClient (singleton separado)
 * Mantemos o cliente Ktor configurado manualmente para requisições customizadas
 * fora do escopo do Supabase SDK.
 */
val networkModule = module {

    single<SupabaseClient> {
        createSupabaseClient(
            supabaseUrl = AppConfig.SUPABASE_URL,
            supabaseKey = AppConfig.SUPABASE_ANON_KEY,
        ) {
            install(Postgrest)
            install(Realtime)
            install(Auth)
        }.also { client ->
            // ── Conexão global do Realtime WebSocket ──────────────────────────
            // Chamada uma única vez no startup. O SupervisorJob garante que uma
            // falha aqui não cancele outros processos do app.
            // O SDK gerencia reconexão automática — não precisamos de lógica extra.
            CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                client.realtime.connect()
            }
        }
    }

    // Ktor HttpClient manual — para requisições fora do Supabase SDK
    single<HttpClient> { createHttpClient() }
}
