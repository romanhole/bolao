package com.bolao.config

/**
 * Ponto de acesso unificado para as chaves de ambiente.
 * Cada plataforma (Android, iOS) injeta seus próprios valores via 'actual'.
 */
expect object AppConfig {
    val SUPABASE_URL: String
    val SUPABASE_ANON_KEY: String
}
