package com.bolao.data.repository

import com.bolao.domain.repository.PushTokenRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable

@Serializable
private data class PushTokenDto(
    val user_id: String,
    val token: String,
    val platform: String = "android",
    val notification_hours_before: Int = 1
)

class PushTokenRepositoryImpl(
    private val supabase: SupabaseClient
) : PushTokenRepository {

    override suspend fun saveToken(userId: String, token: String, hoursBeforeMatch: Int): Result<Unit> {
        return try {
            val dto = PushTokenDto(user_id = userId, token = token, notification_hours_before = hoursBeforeMatch)
            supabase.postgrest["push_tokens"].upsert(dto) {
                onConflict = "user_id, token"
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun deleteToken(token: String): Result<Unit> {
        return try {
            supabase.postgrest["push_tokens"].delete {
                filter {
                    eq("token", token)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun deleteAllTokensForUser(userId: String): Result<Unit> {
        return try {
            supabase.postgrest["push_tokens"].delete {
                filter {
                    eq("user_id", userId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
