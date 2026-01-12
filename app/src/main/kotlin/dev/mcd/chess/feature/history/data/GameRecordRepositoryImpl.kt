package dev.mcd.chess.feature.history.data

import dev.mcd.chess.feature.history.domain.GameRecord
import dev.mcd.chess.feature.history.domain.GameRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlinx.serialization.decodeFromString


class GameRecordRepositoryImpl @Inject constructor(
    private val appPreferences: AppPreferences,
) : GameRecordRepository {

    private val json = Json { encodeDefaults = true }

    override fun recordsUpdates(): Flow<List<GameRecord>> {
        return flow {
            emitAll(
                appPreferences.gameRecordsUpdates().map { stored ->
                    decodeRecords(stored)
                },
            )
        }
    }

    override suspend fun records(): List<GameRecord> {
        return decodeRecords(appPreferences.gameRecords())
    }

    override suspend fun addRecord(record: GameRecord) {
        val records = records().toMutableList()
        records.add(0, record)
        appPreferences.setGameRecords(json.encodeToString(records))
    }

    private fun decodeRecords(stored: String?): List<GameRecord> {
        if (stored.isNullOrBlank()) {
            return emptyList()
        }
        return runCatching { json.decodeFromString<List<GameRecord>>(stored) }
            .getOrDefault(emptyList())
    }
}
