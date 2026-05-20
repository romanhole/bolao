package com.bolao.data.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Factory function expect/actual para criar o [HttpClient] com engine
 * específico para cada plataforma (Android usa OkHttp, iOS usa Darwin).
 *
 * A configuração comum (serialização, logging) é definida aqui em commonMain.
 */
expect fun createHttpClient(): HttpClient

/**
 * Configuração JSON compartilhada para kotlinx.serialization.
 *
 * - [ignoreUnknownKeys]: protege contra campos extras adicionados pelo backend.
 * - [isLenient]:        aceita JSON ligeiramente malformado (útil em dev).
 * - [prettyPrint]:      facilita debug; desabilitar em produção para performance.
 */
internal val sharedJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = false
    encodeDefaults = true
}

/**
 * Extensão interna para aplicar as configurações comuns de Ktor
 * num [HttpClient] após a criação com engine específico de plataforma.
 */
internal fun HttpClient.applyCommonConfig(): HttpClient = config {
    install(ContentNegotiation) {
        json(sharedJson)
    }
    install(Logging) {
        logger   = Logger.SIMPLE
        level    = LogLevel.HEADERS  // Mude para LogLevel.BODY em debug profundo
    }
}
