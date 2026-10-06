package com.bolao.presentation

/**
 * Identificadores estáveis usados em `Modifier.testTag(...)`.
 *
 * Os testes de UI localizam elementos por estas tags em vez de textos visíveis,
 * que mudam com frequência (copy, tradução). Alterar um valor aqui quebra os
 * testes de propósito: a tag é parte do contrato da tela.
 */
object TestTags {

    // ── Ligas ────────────────────────────────────────────────────────────────
    const val LEAGUES_LOADING = "leagues_loading"
    const val LEAGUES_ERROR = "leagues_error"
    const val LEAGUES_EMPTY = "leagues_empty"
    const val LEAGUES_CREATE_BUTTON = "leagues_create_button"
    const val LEAGUES_JOIN_BUTTON = "leagues_join_button"
    const val CREATE_LEAGUE_NAME_FIELD = "create_league_name_field"
    const val CREATE_LEAGUE_NICKNAME_FIELD = "create_league_nickname_field"
    const val CREATE_LEAGUE_CONFIRM = "create_league_confirm"
    const val JOIN_LEAGUE_CODE_FIELD = "join_league_code_field"
    const val JOIN_LEAGUE_NICKNAME_FIELD = "join_league_nickname_field"
    const val JOIN_LEAGUE_CONFIRM = "join_league_confirm"

    fun leagueCard(leagueId: String) = "league_card_$leagueId"

    // ── Palpites ─────────────────────────────────────────────────────────────
    const val MATCHES_LOADING = "matches_loading"
    const val MATCHES_ERROR = "matches_error"

    fun matchCard(matchId: String) = "match_card_$matchId"
    fun goalIncrement(matchId: String, side: String) = "goal_${side}_increment_$matchId"
    fun goalDecrement(matchId: String, side: String) = "goal_${side}_decrement_$matchId"
    fun goalValue(matchId: String, side: String) = "goal_${side}_value_$matchId"
    fun saveButton(matchId: String) = "save_prediction_$matchId"

    const val SIDE_HOME = "home"
    const val SIDE_AWAY = "away"

    // ── Detalhe da liga / classificação ──────────────────────────────────────
    const val LEAGUE_DETAIL_LOADING = "league_detail_loading"
    const val LEAGUE_DETAIL_ERROR = "league_detail_error"
    const val LEAGUE_DETAIL_TITLE = "league_detail_title"
    const val RANKING_ROW_PREFIX = "ranking_row_"

    fun rankingRow(userId: String) = "$RANKING_ROW_PREFIX$userId"
}
