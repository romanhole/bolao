package com.bolao.config

/**
 * Implementação WebAssembly das chaves de ambiente.
 * Para a versão PWA, estamos usando o ambiente de Produção diretamente.
 */
import kotlinx.browser.window

actual object AppConfig {
    actual val SUPABASE_URL: String
        get() = if (isDevEnvironment()) {
            "https://aecvtidljkwydmopbsgd.supabase.co"
        } else {
            "https://uetdonnoytbsjvuliiec.supabase.co"
        }

    actual val SUPABASE_ANON_KEY: String
        get() = if (isDevEnvironment()) {
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFlY3Z0aWRsamt3eWRtb3Bic2dkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzkyMTg5OTUsImV4cCI6MjA5NDc5NDk5NX0.0k6bELat4llEYjcxuSP51K1F1UNb5rq4KQuzycOajbs"
        } else {
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVldGRvbm5veXRic2p2dWxpaWVjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzk0NjMyMzIsImV4cCI6MjA5NTAzOTIzMn0.aBeDb9mUJlrtH0VNip8lSTpJ7hVpthhL3Nj2IhMOGRk"
        }

    private fun isDevEnvironment(): Boolean {
        val hostname = try { window.location.hostname } catch (e: Exception) { "" }
        return hostname.contains("localhost") || hostname.contains("127.0.0.1") || hostname.contains("vercel.app")
    }
}
