package com.bolao.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android

/**
 * Implementação Android do [createHttpClient].
 * Usa o engine `Android` (baseado em HttpURLConnection).
 * Para projetos com OkHttp, substitua por io.ktor:ktor-client-okhttp.
 */
actual fun createHttpClient(): HttpClient =
    HttpClient(Android) {
        engine {
            connectTimeout = 15_000
            socketTimeout  = 30_000
        }
    }.applyCommonConfig()
