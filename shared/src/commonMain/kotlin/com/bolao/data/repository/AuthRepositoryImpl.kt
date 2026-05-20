package com.bolao.data.repository

import com.bolao.domain.model.UserSession
import com.bolao.domain.repository.AuthRepository
import com.bolao.domain.repository.AuthState
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementação real de [AuthRepository] usando o Supabase Auth SDK (auth-kt v3.x).
 *
 * ## Persistência de Sessão
 * O SDK do Supabase Auth persiste automaticamente a sessão no armazenamento do dispositivo
 * (SharedPreferences no Android, Keychain no iOS). Ao reabrir o app, a sessão é restaurada
 * e [authState] emite [AuthState.Loading] → [AuthState.Authenticated] automaticamente.
 *
 * ## Mapeamento de SessionStatus → AuthState
 * ```
 * SDK SessionStatus.LoadingFromStorage → AuthState.Loading
 * SDK SessionStatus.Authenticated      → AuthState.Authenticated(UserSession)
 * SDK SessionStatus.NotAuthenticated   → AuthState.NotAuthenticated
 * ```
 *
 * ## signInWith / signUpWith
 * Ambas são suspend functions. Em caso de erro (senha errada, e-mail duplicado, etc.),
 * lançam exceções que são capturadas pelo [runCatching] e retornadas como [Result.failure].
 */
class AuthRepositoryImpl(
    private val supabase: SupabaseClient,
) : AuthRepository {

    override val authState: Flow<AuthState> = supabase.auth.sessionStatus
        .map { status ->
            when (status) {
                is SessionStatus.Initializing ->
                    AuthState.Loading

                is SessionStatus.Authenticated -> {
                    val sdkUser = status.session.user
                    if (sdkUser != null) {
                        AuthState.Authenticated(
                            UserSession(
                                userId = sdkUser.id,
                                email  = sdkUser.email ?: "",
                            )
                        )
                    } else {
                        AuthState.NotAuthenticated
                    }
                }

                else -> AuthState.NotAuthenticated
            }
        }

    override val currentUser: Flow<UserSession?> = authState.map { state ->
        (state as? AuthState.Authenticated)?.user
    }

    override suspend fun login(email: String, password: String): Result<Unit> =
        runCatching {
            supabase.auth.signInWith(Email) {
                this.email    = email
                this.password = password
            }
        }

    override suspend fun signUp(email: String, password: String): Result<Unit> =
        runCatching {
            supabase.auth.signUpWith(Email) {
                this.email    = email
                this.password = password
            }
        }

    override suspend fun logout() {
        supabase.auth.signOut()
    }
}
