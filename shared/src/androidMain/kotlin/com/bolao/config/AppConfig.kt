package com.bolao.config

import com.bolao.shared.BuildConfig

/**
 * Implementação Android das chaves de ambiente.
 * Puxa automaticamente as variáveis geradas pelo Gradle BuildConfig,
 * respeitando os Build Variants do Android Studio (Debug vs Release).
 */
actual object AppConfig {
    actual val SUPABASE_URL: String = BuildConfig.SUPABASE_URL
    actual val SUPABASE_ANON_KEY: String = BuildConfig.SUPABASE_ANON_KEY
}
