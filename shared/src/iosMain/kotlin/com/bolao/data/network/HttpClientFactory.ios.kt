package com.bolao.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

/**
 * Implementação iOS do [createHttpClient].
 * Usa o engine `Darwin` (NSURLSession nativo do iOS/macOS).
 */
actual fun createHttpClient(): HttpClient =
    HttpClient(Darwin) {
        engine {
            configureRequest {
                setAllowsCellularAccess(true)
            }
        }
    }.applyCommonConfig()
