package com.bolao.config

/**
 * Implementação WebAssembly das chaves de ambiente.
 * Para a versão PWA, estamos usando o ambiente de Produção diretamente.
 */
actual object AppConfig {
    actual val SUPABASE_URL: String = "https://uetdonnoytbsjvuliiec.supabase.co"
    actual val SUPABASE_ANON_KEY: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVldGRvbm5veXRic2p2dWxpaWVjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzk0NjMyMzIsImV4cCI6MjA5NTAzOTIzMn0.aBeDb9mUJlrtH0VNip8lSTpJ7hVpthhL3Nj2IhMOGRk"
}
