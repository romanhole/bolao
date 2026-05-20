package com.bolao.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import java.util.concurrent.TimeUnit

/**
 * Implementação Android do [createHttpClient].
 *
 * ## Por que OkHttp em vez de Android (HttpURLConnection)?
 * O engine `Android` do Ktor usa `HttpURLConnection` que **não implementa
 * [WebSocketCapability]**. O Supabase Realtime precisa de WebSockets para
 * receber eventos em tempo real — por isso a engine OkHttp é obrigatória.
 *
 * OkHttp suporta HTTP/1.1, HTTP/2, WebSockets e tem reconexão automática.
 */
actual fun createHttpClient(): HttpClient =
    HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(15, TimeUnit.SECONDS)
                readTimeout(30, TimeUnit.SECONDS)
                writeTimeout(30, TimeUnit.SECONDS)
            }
        }
    }.applyCommonConfig()

