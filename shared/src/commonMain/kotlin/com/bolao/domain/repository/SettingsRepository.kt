package com.bolao.domain.repository

import com.bolao.domain.model.AppSettings

interface SettingsRepository {
    /** Busca as configurações gerais do app (ex: controle de versão de atualizações) */
    suspend fun getSettings(): Result<AppSettings>
}
