package com.bolao.data.repository

import com.bolao.data.remote.dto.SettingsDto
import com.bolao.domain.model.AppSettings
import com.bolao.domain.repository.SettingsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class SettingsRepositoryImpl(
    private val supabase: SupabaseClient
) : SettingsRepository {

    override suspend fun getSettings(): Result<AppSettings> = runCatching {
        val dto = supabase.postgrest["app_settings"]
            .select()
            .decodeSingle<SettingsDto>()
            
        AppSettings(
            minVersionCode = dto.minVersionCode,
            latestVersionCode = dto.latestVersionCode
        )
    }
}
