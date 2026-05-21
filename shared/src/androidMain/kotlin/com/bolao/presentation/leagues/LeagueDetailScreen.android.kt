package com.bolao.presentation.leagues

import android.content.Intent
import com.bolao.platform.AppContext
import com.bolao.domain.model.League

/**
 * Compartilha o código de convite da liga usando o ACTION_SEND do Android.
 * O usuário pode escolher WhatsApp, Telegram, SMS, etc.
 */
actual fun shareLeagueInvite(league: League) {
    val text = "Entre na minha liga '${league.name}' no Bolão! Código: ${league.inviteCode}"
    val chooser = Intent.createChooser(
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
        "Compartilhar convite"
    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    AppContext.get().startActivity(chooser)
}

