package com.bolao.data.repository

import com.bolao.data.remote.dto.LeagueDto
import com.bolao.data.remote.dto.LeagueMemberDto
import com.bolao.domain.model.League
import com.bolao.domain.repository.AuthRepository
import com.bolao.domain.repository.LeagueRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlin.random.Random

class LeagueRepositoryImpl(
    private val supabase: SupabaseClient,
    private val authRepository: AuthRepository,
) : LeagueRepository {

    override fun getUserLeagues(): Flow<List<League>> = flow {
        val userId = authRepository.requireUserId()

        // Canal Realtime para ser notificado quando for adicionado em novas ligas
        val channel = supabase.channel("league_members_changes")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "league_members"
            filter("user_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, userId)
        }
        
        channel.subscribe()

        // Emite a lista inicial e depois reage a cada mudança na tabela
        val updates = changes
            .map { fetchLeaguesForUser(userId) }
            .onStart { emit(fetchLeaguesForUser(userId)) }

        emitAll(updates)
    }

    private suspend fun fetchLeaguesForUser(userId: String): List<League> {
        // Para simplificar, faremos um fetch duplo ou uma chamada com select(leagues(*))
        // PostgREST suporta joins. A tabela `league_members` tem uma FK para `leagues`.
        // Vamos buscar as ligas através dos membros.
        val dtos = supabase.postgrest["leagues"]
            .select {
                // Sintaxe PostgREST para inner join passando pela tabela associativa
                // Assumindo que a relação liga a tabela de members ao id do usuario
                // "leagues!inner(league_members!inner(user_id=eq.$userId))" não é trivial sem definir as relações corretas no banco
                // Uma forma mais segura sem saber o nome exato das chaves estrangeiras:
            }
            
        // Como a forma de join requer o nome exato do relacionamento do PostgREST,
        // vamos fazer em dois passos para garantir que vai funcionar com o RLS simples:
        val members = supabase.postgrest["league_members"]
            .select { filter { eq("user_id", userId) } }
            .decodeList<LeagueMemberDto>()

        if (members.isEmpty()) return emptyList()

        val leagueIds = members.map { it.leagueId }

        val leagues = supabase.postgrest["leagues"]
            .select { filter { isIn("id", leagueIds) } }
            .decodeList<LeagueDto>()

        return leagues.map {
            League(
                id = it.id ?: "",
                name = it.name,
                inviteCode = it.inviteCode,
                ownerId = it.ownerId
            )
        }
    }

    override suspend fun createLeague(name: String): Result<League> = runCatching {
        val userId = authRepository.requireUserId()
        
        // Gera código de 6 caracteres maiúsculos + números
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val inviteCode = (1..6).map { chars.random() }.joinToString("")

        val dto = LeagueDto(
            name = name,
            inviteCode = inviteCode,
            ownerId = userId
        )

        // Cria a liga
        val created = supabase.postgrest["leagues"]
            .insert(dto) { select() }
            .decodeSingle<LeagueDto>()
            
        val leagueId = created.id ?: throw Exception("Falha ao recuperar ID da liga criada")

        // Insere o owner como membro
        val memberDto = LeagueMemberDto(
            leagueId = leagueId,
            userId = userId
        )
        supabase.postgrest["league_members"].insert(memberDto)

        League(
            id = leagueId,
            name = created.name,
            inviteCode = created.inviteCode,
            ownerId = created.ownerId
        )
    }

    override suspend fun joinLeague(inviteCode: String): Result<Unit> = runCatching {
        val userId = authRepository.requireUserId()

        // 1. Busca a liga pelo invite_code
        val leagues = supabase.postgrest["leagues"]
            .select { filter { eq("invite_code", inviteCode.uppercase()) } }
            .decodeList<LeagueDto>()

        if (leagues.isEmpty()) {
            throw Exception("Código de convite inválido ou liga não encontrada.")
        }

        val leagueId = leagues.first().id!!

        // 2. Verifica se já é membro
        val existing = supabase.postgrest["league_members"]
            .select { 
                filter { 
                    eq("league_id", leagueId)
                    eq("user_id", userId)
                } 
            }
            .decodeList<LeagueMemberDto>()
            
        if (existing.isNotEmpty()) {
            throw Exception("Você já participa desta liga.")
        }

        // 3. Entra na liga
        val memberDto = LeagueMemberDto(
            leagueId = leagueId,
            userId = userId
        )
        
        supabase.postgrest["league_members"].insert(memberDto)
    }
    
    private suspend fun AuthRepository.requireUserId(): String {
        return authState.first { it is com.bolao.domain.repository.AuthState.Authenticated }
            .let { (it as com.bolao.domain.repository.AuthState.Authenticated).user.userId }
    }
}
