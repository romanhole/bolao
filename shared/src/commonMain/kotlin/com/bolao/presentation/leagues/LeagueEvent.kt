package com.bolao.presentation.leagues

/**
 * Eventos tipados emitidos pelo [LeaguesViewModel] para a UI.
 *
 * Substituem o simples [String] do SharedFlow, permitindo que a UI
 * tome ações distintas dependendo do tipo de evento.
 */
sealed interface LeagueEvent {
    /** Exibe uma Snackbar com a mensagem fornecida. */
    data class ShowMessage(val message: String) : LeagueEvent

    /** Navega imediatamente para a LeagueDetailScreen da liga criada. */
    data class NavigateToLeague(val leagueId: String) : LeagueEvent
}
