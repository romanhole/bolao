package com.bolao.domain.repository

import com.bolao.domain.model.Match
import kotlinx.coroutines.flow.Flow

/**
 * Contrato do repositório de partidas.
 *
 * ARQUITETURA — REGRA DE OURO:
 * Esta interface descreve *o que* a camada de dados deve fornecer.
 * A implementação concreta (em data/repository/) consumirá EXCLUSIVAMENTE
 * o nosso backend (Firestore / Supabase) e NUNCA chamará APIs de futebol externas.
 *
 * Todos os métodos retornam [Flow] para suportar atualizações em tempo real
 * via listeners do Firestore ou WebSocket do Supabase.
 */
interface MatchRepository {

    /**
     * Observa a lista de partidas de uma determinada competição.
     * Emite novos valores sempre que os dados mudam no backend (ex: placar ao vivo).
     *
     * @param competitionId ID da competição no nosso banco de dados.
     */
    fun observeMatchesByCompetition(competitionId: String): Flow<List<Match>>

    /**
     * Observa os detalhes de uma partida específica em tempo real.
     *
     * @param matchId ID da partida no nosso banco de dados.
     */
    fun observeMatchById(matchId: String): Flow<Match?>

    /**
     * Busca a lista de partidas de uma rodada.
     * Útil para a tela inicial agrupada por rodada.
     *
     * @param competitionId ID da competição.
     * @param round         Identificador da rodada (ex: "1", "quartas").
     */
    fun observeMatchesByRound(competitionId: String, round: String): Flow<List<Match>>
}
