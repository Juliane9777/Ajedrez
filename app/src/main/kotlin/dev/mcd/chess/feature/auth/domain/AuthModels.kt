package dev.mcd.chess.feature.auth.domain

import kotlinx.serialization.Serializable

@Serializable
data class AuthSession(
    val username: String,
    val role: UserRole,
)

@Serializable
data class StoredUser(
    val username: String,
    val password: String,
    val role: UserRole,
)

@Serializable
enum class UserRole {
    User,
    Admin,
}
