package com.bolao.data.repository

import com.bolao.data.remote.dto.MatchDto
import com.bolao.data.remote.mapper.toDomain
import com.bolao.domain.model.Match
import com.bolao.domain.repository.MatchRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Implementação real de [MatchRepository] usando o Supabase como backend.
 *
 * ## Estratégia de Realtime + Join
 *
 * O Supabase Realtime entrega eventos de mudança de linha (`INSERT`, `UPDATE`,
 * `DELETE`) para a tabela `matches`, mas **não inclui dados de tabelas relacionadas
 * (join)**. Isso cria o problema: ao receber um evento de update do placar, não
 * temos os dados dos times no payload.
 *
 * Solução adotada: **re-fetch completo** após qualquer evento Realtime.
 *
 * ```
 * Evento Realtime (UPDATE em matches)
 *     ↓
 * Re-fetch: SELECT * + home_team JOIN + away_team JOIN
 *     ↓
 * Emit via channelFlow → ViewModel → UI atualiza
 * ```
 *
 * Para um bolão com ~30-50 partidas por competição, isso é perfeitamente
 * eficiente. Se a escala crescer, otimize com cache local de times.
 *
 * ## channelFlow vs callbackFlow
 * `channelFlow` é usado porque precisamos lançar a subscription Realtime
 * como uma coroutine lateral (`launchIn(this)`) enquanto o flow principal
 * aguarda novos valores via `send()`.
 */
class MatchRepositoryImpl(
    private val supabase: SupabaseClient,
) : MatchRepository {

    companion object {
        private const val TABLE = "matches"

        /**
         * Colunas selecionadas com join de times via PostgREST.
         *
         * Sintaxe: `alias:tabela!chave_estrangeira(colunas)`
         * - `home_team:teams!home_team_id(...)` → join pelo FK `home_team_id`
         * - `away_team:teams!away_team_id(...)` → join pelo FK `away_team_id`
         *
         * Os aliases `home_team` e `away_team` devem corresponder ao
         * `@SerialName` em [MatchDto].
         */
        private val COLUMNS_WITH_TEAMS = Columns.raw(
            """
            id,
            home_score, away_score,
            home_odd, draw_odd, away_odd, stage_multiplier,
            status, minute_played, interrupted_reason,
            scheduled_at, competition_id, competition, round,
            home_team:teams!home_team_id(id, name, short_name, logo_url, api_team_id),
            away_team:teams!away_team_id(id, name, short_name, logo_url, api_team_id)
            """.trimIndent()
        )
    }

    // ── observeMatchesByCompetition ────────────────────────────────────────────

    override fun observeMatchesByCompetition(competitionId: String): Flow<List<Match>> =
        channelFlow {
            // 1. Emissão inicial — carrega os dados antes de conectar ao Realtime
            send(fetchMatches(competitionId))

            // 2. Canal Realtime para a tabela matches
            //    O nome do canal deve ser único por conexão; inclui competitionId
            //    para suportar múltiplas competições abertas simultaneamente
            val channel = supabase.channel("matches-competition-$competitionId")

            // 3. Subscribe ao flow de eventos Postgres Change (INSERT/UPDATE/DELETE)
            //    Ao receber qualquer evento, re-faz o fetch completo com joins
            channel
                .postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = TABLE
                }
                .onEach {
                    // Re-fetch completo: necessário para incluir dados dos times (join)
                    trySend(fetchMatches(competitionId))
                }
                .launchIn(this) // Roda como coroutine filha do channelFlow scope

            channel.subscribe()

            // 4. awaitClose: cleanup quando o Flow for cancelado (ex: usuário sai da tela)
            awaitClose {
                launch {
                    supabase.realtime.removeChannel(channel)
                }
            }
        }

    // ── observeMatchById ──────────────────────────────────────────────────────

    override fun observeMatchById(matchId: String): Flow<Match?> = channelFlow {
        send(fetchMatchById(matchId))

        val channel = supabase.channel("match-detail-$matchId")

        channel
            .postgresChangeFlow<PostgresAction>(schema = "public") {
                table = TABLE
                // Filtro server-side: só recebe eventos para esta partida específica
                // Reduz tráfego de rede desnecessário
                filter("id", FilterOperator.EQ, matchId)
            }
            .onEach { trySend(fetchMatchById(matchId)) }
            .launchIn(this)

        channel.subscribe()
        awaitClose {
            launch {
                supabase.realtime.removeChannel(channel)
            }
        }
    }

    // ── observeMatchesByRound ─────────────────────────────────────────────────

    /**
     * Filtra as partidas por rodada a partir do flow de competição.
     * Reutiliza a subscription Realtime de [observeMatchesByCompetition].
     */
    override fun observeMatchesByRound(
        competitionId: String,
        round: String,
    ): Flow<List<Match>> =
        observeMatchesByCompetition(competitionId)
            .map { matches -> matches.filter { it.round == round } }

    // ── Helpers privados ──────────────────────────────────────────────────────

    /**
     * Busca todas as partidas de uma competição com dados dos times embutidos.
     * Ordenadas por `scheduled_at` ascendente (próximas primeiro).
     */
    private suspend fun fetchMatches(competitionId: String): List<Match> =
        supabase
            .from(TABLE)
            .select(columns = COLUMNS_WITH_TEAMS) {
                filter { eq("competition_id", competitionId) }
                order(column = "scheduled_at", order = Order.ASCENDING)
            }
            .decodeList<MatchDto>()
            .map { it.toDomain() }

    /**
     * Busca uma partida específica com join de times.
     * Retorna `null` se o ID não existir no banco.
     */
    private suspend fun fetchMatchById(matchId: String): Match? =
        supabase
            .from(TABLE)
            .select(columns = COLUMNS_WITH_TEAMS) {
                filter { eq("id", matchId) }
                limit(count = 1)
            }
            .decodeList<MatchDto>()
            .firstOrNull()
            ?.toDomain()
}
