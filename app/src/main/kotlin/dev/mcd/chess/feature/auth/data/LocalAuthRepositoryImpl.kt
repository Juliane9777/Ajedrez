package dev.mcd.chess.feature.auth.data

import dev.mcd.chess.feature.auth.domain.AuthResult
import dev.mcd.chess.feature.auth.domain.AuthSession
import dev.mcd.chess.feature.auth.domain.LocalAuthRepository
import dev.mcd.chess.feature.auth.domain.StoredUser
import dev.mcd.chess.feature.auth.domain.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class LocalAuthRepositoryImpl @Inject constructor(
    private val appPreferences: AppPreferences,
) : LocalAuthRepository {

    private val json = Json { encodeDefaults = true }
    private val defaultUsers = listOf(
        StoredUser(username = "admin", password = "admin", role = UserRole.Admin),
        StoredUser(username = "usuario1", password = "usuario1", role = UserRole.User),
    )

    override fun sessionUpdates(): Flow<AuthSession?> {
        return flow {
            emitAll(
                appPreferences.currentUserUpdates().map { username ->
                    username?.let { findUser(it) }?.toSession()
                },
            )
        }
    }

    override suspend fun session(): AuthSession? {
        val username = appPreferences.currentUser() ?: return null
        return findUser(username)?.toSession()
    }

    override suspend fun login(username: String, password: String): AuthResult {
        val user = findUser(username)
        return if (user == null || user.password != password) {
            AuthResult.Error("Credenciales inválidas")
        } else {
            appPreferences.setCurrentUser(user.username)
            AuthResult.Success(user.toSession())
        }
    }

    override suspend fun register(username: String, password: String): AuthResult {
        if (username.isBlank() || password.isBlank()) {
            return AuthResult.Error("Usuario y contraseña obligatorios")
        }
        val users = loadUsers().toMutableList()
        if (users.any { it.username.equals(username, ignoreCase = true) }) {
            return AuthResult.Error("El usuario ya existe")
        }
        val newUser = StoredUser(username = username, password = password, role = UserRole.User)
        users.add(newUser)
        saveUsers(users)
        appPreferences.setCurrentUser(newUser.username)
        return AuthResult.Success(newUser.toSession())
    }

    override suspend fun logout() {
        appPreferences.setCurrentUser(null)
    }

    private suspend fun findUser(username: String): StoredUser? {
        return loadUsers().firstOrNull { it.username.equals(username, ignoreCase = true) }
    }

    private suspend fun loadUsers(): List<StoredUser> {
        val stored = appPreferences.authUsers()
        if (stored.isNullOrBlank()) {
            saveUsers(defaultUsers)
            return defaultUsers
        }
        return runCatching { json.decodeFromString<List<StoredUser>>(stored) }.getOrElse {
            saveUsers(defaultUsers)
            defaultUsers
        }
    }

    private suspend fun saveUsers(users: List<StoredUser>) {
        appPreferences.setAuthUsers(json.encodeToString(users))
    }

    private fun StoredUser.toSession(): AuthSession {
        return AuthSession(username = username, role = role)
    }
}
