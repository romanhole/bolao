package com.bolao.presentation.leagues

import com.bolao.domain.model.League
import kotlinx.browser.window

/**
 * Na versão web, usamos a API nativa do navegador para exibir o código.
 */
actual fun shareLeagueInvite(league: League) {
    window.alert("Participe da minha liga no Bolão!\n\nCódigo: ${league.inviteCode}")
}
