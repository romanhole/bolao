package com.bolao.data.repository

import com.bolao.data.remote.dto.PredictionDto
import com.bolao.data.remote.mapper.toDomain
import com.bolao.data.remote.mapper.toDto
import com.bolao.domain.model.Prediction
import com.bolao.domain.repository.PredictionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Implementação real de [PredictionRepository] usando o Supabase.
 *
 * ## Upsert Strategy
 * `savePrediction` usa `upsert` com `ON CONFLICT (match_id, user_id)` para:
 * - **Primeiro palpite**: INSERT → banco gera UUID automático
 * - **Atualização**: UPDATE no registro existente (mesma constraint)
 *
 * Após o upsert, fazemos um re-fetch para obter o registro com o ID gerado.
 *
 * ## Realtime de Pontuação
 * Quando o backend calcula os pontos (via trigger ou Edge Function) e atualiza
 * `points_earned` na tabela `predictions`, o evento Realtime dispara e o
 * flow emite a lista atualizada — a UI exibe o badge dourado automaticamente.
 *
 * ## competitionId em observePredictionsByUser
 * A tabela `predictions` não tem `competition_id` diretamente — ela referencia
 * `matches`. Para filtrar por competição, seria necessário um join com `matches`.
 * Na implementação atual, retornamos TODOS os palpites do usuário e deixamos
 * o ViewModel filtrar implicitamente (ele só processa partidas da competição ativa).
 * TODO: Adicionar join com matches para filtro mais eficiente em produção.
 */
class PredictionRepositoryImpl(
    private val supabase: SupabaseClient,
) : PredictionRepository {

    companion object {
        private const val TABLE = "predictions"
    }

    // ── observePredictionsByUser ───────────────────────────────────────────────

    override fun observePredictionsByUser(
        userId: String,
        competitionId: String,
    ): Flow<List<Prediction>> = channelFlow {
        // Emissão inicial
        send(fetchPredictionsByUser(userId))
        val channel = supabase.channel("predictions-user-$userId")

        channel
            .postgresChangeFlow<PostgresAction>(schema = "public") {
                table = TABLE
                // Filtro server-side: só eventos do usuário logado
                // Garante que um usuário não receba eventos de palpites de terceiros
                filter("user_id", FilterOperator.EQ, userId)
            }
            .onEach { trySend(fetchPredictionsByUser(userId)) }
            .launchIn(this)

        channel.subscribe()
        awaitClose {
            launch {
                supabase.realtime.removeChannel(channel)
            }
        }
    }

    // ── observePredictionForMatch ──────────────────────────────────────────────

    override fun observePredictionForMatch(
        userId: String,
        matchId: String,
    ): Flow<Prediction?> = channelFlow {
        send(fetchPredictionForMatch(userId, matchId))

        val channel = supabase.channel("prediction-$userId-$matchId")

        channel
            .postgresChangeFlow<PostgresAction>(schema = "public") {
                table = TABLE
                filter("user_id", FilterOperator.EQ, userId)
            }
            .onEach { trySend(fetchPredictionForMatch(userId, matchId)) }
            .launchIn(this)

        channel.subscribe()
        awaitClose {
            launch {
                supabase.realtime.removeChannel(channel)
            }
        }
    }

    // ── savePrediction ────────────────────────────────────────────────────────

    /**
     * Persiste um palpite com upsert no Supabase.
     *
     * O `onConflict = "match_id,user_id"` instrui o PostgREST a atualizar
     * o registro existente em vez de falhar em caso de conflito — equivalente
     * ao `INSERT ... ON CONFLICT (match_id, user_id) DO UPDATE SET ...`.
     *
     * Envolvido em `runCatching` para capturar erros de rede ou violações
     * de constraint e retornar `Result.failure` sem crashar o app.
     */
    override suspend fun savePrediction(prediction: Prediction): Result<Prediction> =
        runCatching {
            supabase
                .from(TABLE)
                .upsert(prediction.toDto()) {
                    onConflict   = "match_id,user_id"
                    defaultToNull = false   // preserva colunas não enviadas (ex: points_earned)
                }

            // Re-fetch após upsert para obter o ID gerado pelo banco e estado atual
            fetchPredictionForMatch(prediction.userId, prediction.matchId)
                ?: error("Palpite não encontrado após upsert — verifique as RLS policies")
        }

    // ── observeLeaderboard ────────────────────────────────────────────────────

    /**
     * Ranking de pontos para uma competição.
     *
     * Ordenado por `points_earned DESC` — usuários sem pontos (null) ficam por último
     * (PostgreSQL trata NULL como menor em ORDER BY DESC sem NULLS FIRST/LAST).
     *
     * TODO: Adicionar join com tabela de usuários para exibir nome/avatar no ranking.
     */
    override fun observeLeaderboard(competitionId: String): Flow<List<Prediction>> =
        channelFlow {
            send(fetchLeaderboard())
            val channel = supabase.channel("leaderboard-$competitionId")

            channel
                .postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = TABLE
                }
                .onEach { trySend(fetchLeaderboard()) }
                .launchIn(this)

            channel.subscribe()
            awaitClose {
                launch {
                    supabase.realtime.removeChannel(channel)
                }
            }
        }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private suspend fun fetchPredictionsByUser(userId: String): List<Prediction> =
        supabase
            .from(TABLE)
            .select {
                filter { eq("user_id", userId) }
            }
            .decodeList<PredictionDto>()
            .map { it.toDomain() }

    private suspend fun fetchPredictionForMatch(
        userId: String,
        matchId: String,
    ): Prediction? =
        supabase
            .from(TABLE)
            .select {
                filter {
                    eq("user_id", userId)
                    eq("match_id", matchId)
                }
                limit(count = 1)
            }
            .decodeList<PredictionDto>()
            .firstOrNull()
            ?.toDomain()

    private suspend fun fetchLeaderboard(): List<Prediction> =
        supabase
            .from(TABLE)
            .select {
                order(column = "points_earned", order = Order.DESCENDING)
            }
            .decodeList<PredictionDto>()
            .map { it.toDomain() }
}
