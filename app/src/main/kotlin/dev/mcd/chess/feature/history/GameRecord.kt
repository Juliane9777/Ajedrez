package dev.mcd.chess.feature.history.domain

import kotlinx.serialization.Serializable

@Serializable
data class GameRecord(
    val id: String,
    val username: String,
    val mode: GameRecordMode,
    val moves: List<String>,
    val result: String,
    val createdAt: Long,
)

@Serializable
enum class GameRecordMode {
    Online,
    Bot,
    Puzzle,
}
