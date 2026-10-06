package com.bolao.testing

import com.bolao.domain.model.UserSession
import com.bolao.domain.repository.AuthRepository
import com.bolao.domain.repository.AuthState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Usuário sempre autenticado com [user]; nenhuma chamada ao Supabase Auth. */
class FakeAuthRepository(user: UserSession = TestData.currentUser) : AuthRepository {

    private val state = MutableStateFlow<AuthState>(AuthState.Authenticated(user))

    override val authState: Flow<AuthState> = state
    override val currentUser: Flow<UserSession?> = state.map { (it as? AuthState.Authenticated)?.user }
    override val deepLinkError: Flow<String?> = MutableStateFlow<String?>(null)

    override suspend fun login(email: String, password: String): Result<Unit> = Result.success(Unit)
    override suspend fun signUp(email: String, password: String): Result<Unit> = Result.success(Unit)
    override suspend fun resendConfirmationEmail(email: String): Result<Unit> = Result.success(Unit)
    override suspend fun logout() {
        state.value = AuthState.NotAuthenticated
    }
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = Result.success(Unit)
    override suspend fun verifyPasswordResetOtp(email: String, otp: String): Result<Unit> = Result.success(Unit)
    override suspend fun updatePassword(newPassword: String): Result<Unit> = Result.success(Unit)
    override suspend fun handleDeepLink(url: String) = Unit
    override fun clearDeepLinkError() = Unit
}
