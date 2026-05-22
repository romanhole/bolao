package com.bolao.domain.repository

import com.bolao.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

/**
 * Estado de autenticação do usuário.
 *
 * - [Loading] — sessão está sendo restaurada do armazenamento persistente
 * - [NotAuthenticated] — usuário não está logado
 * - [Authenticated] — usuário autenticado, com os dados da sessão
 */
sealed interface AuthState {
    data object Loading : AuthState
    data object NotAuthenticated : AuthState
    data class Authenticated(val user: UserSession) : AuthState
}

/**
 * Contrato de autenticação — desacopla a UI e o ViewModel do Supabase SDK.
 *
 * ## Regra de Design
 * Esta interface vive no domínio e não depende de NENHUM framework externo.
 * A implementação concreta ([AuthRepositoryImpl]) usa o Supabase Auth SDK.
 */
interface AuthRepository {

    /**
     * Flow reativo do estado de autenticação.
     * Emite [AuthState.Loading] enquanto a sessão é restaurada,
     * [AuthState.Authenticated] quando logado, ou [AuthState.NotAuthenticated].
     */
    val authState: Flow<AuthState>

    /**
     * Convenience flow — emite o [UserSession] quando autenticado, ou null.
     * Derivado de [authState].
     */
    val currentUser: Flow<UserSession?>

    /**
     * Realiza login com e-mail e senha.
     * @return [Result.success] em caso de sucesso, [Result.failure] com a exceção em caso de erro.
     */
    suspend fun login(email: String, password: String): Result<Unit>

    /**
     * Cria uma nova conta com e-mail, senha e nickname.
     * @return [Result.success] em caso de sucesso, [Result.failure] com a exceção em caso de erro.
     */
    suspend fun signUp(email: String, password: String): Result<Unit>

    /** Encerra a sessão do usuário atual. */
    suspend fun logout()
}
