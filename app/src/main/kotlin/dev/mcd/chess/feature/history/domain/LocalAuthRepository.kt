/*package dev.mcd.chess.feature.auth.domain

import kotlinx.coroutines.flow.Flow

interface LocalAuthRepository {
    fun sessionUpdates(): Flow<AuthSession?>
    suspend fun session(): AuthSession?
    suspend fun login(username: String, password: String): AuthResult
    suspend fun register(username: String, password: String): AuthResult
    suspend fun logout()
}

sealed interface AuthResult {
    data class Success(val session: AuthSession) : AuthResult
    data class Error(val message: String) : AuthResult
}
*/
