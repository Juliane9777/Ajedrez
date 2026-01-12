package dev.mcd.chess.feature.history.domain

import kotlinx.coroutines.flow.Flow

interface GameRecordRepository {
    fun recordsUpdates(): Flow<List<GameRecord>>
    suspend fun records(): List<GameRecord>
    suspend fun addRecord(record: GameRecord)
}
