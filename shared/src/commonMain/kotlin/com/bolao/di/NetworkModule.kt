package com.bolao.di

import com.bolao.data.network.createHttpClient
import com.bolao.data.network.sharedJson
import com.bolao.data.remote.SupabaseConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.ktor.client.HttpClient
import kotlin.time.Duration.Companion.seconds
import org.koin.dsl.module

/**
 * Módulo Koin de rede.
 *
 * ## SupabaseClient (singleton)
 * O cliente Supabase é compartilhado por todos os repositórios.
 * Internamente, o SDK gerencia um pool de conexões Ktor — não crie múltiplas
 * instâncias (custo de memória e conexões abertas desnecessárias).
 *
 * ### Postgrest
 * Habilita queries REST via PostgREST (SELECT, INSERT, UPDATE, UPSERT, DELETE).
 * O serializer padrão do SDK usa `kotlinx.serialization` automaticamente
 * ao detectar classes `@Serializable`.
 *
 * ### Realtime
 * Habilita subscriptions via WebSocket para eventos Postgres Change
 * (INSERT/UPDATE/DELETE em tabelas habilitadas para Realtime no Supabase).
 * [reconnectDelay] define o tempo de espera antes de reconectar após queda.
 *
 * ## HttpClient (singleton separado)
 * Mantemos o cliente Ktor configurado manualmente para:
 * - Chamadas a Firebase Cloud Functions (se futuramente usarmos em conjunto)
 * - Requests customizados que precisem de headers específicos
 * - Funciona com o padrão expect/actual por plataforma (Android/iOS engines)
 */
val networkModule = module {

    single<SupabaseClient> {
        createSupabaseClient(
            supabaseUrl = SupabaseConfig.URL,
            supabaseKey = SupabaseConfig.ANON_KEY,
        ) {
            install(Postgrest)

            install(Realtime)
        }
    }

    // Ktor HttpClient manual — para requisições fora do Supabase SDK
    single<HttpClient> { createHttpClient() }
}
