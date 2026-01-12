package dev.mcd.chess.feature.common.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.mcd.chess.common.player.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore("prefs")

class AppPreferencesImpl @Inject constructor(
    @ApplicationContext context: Context,
) : AppPreferences {

    private val store = context.dataStore
    private val hostKey = stringPreferencesKey("debug-host")
    private val clickToMove = booleanPreferencesKey("click-to-move")
    private val tokenKey = stringPreferencesKey("token")
    private val userKey = stringPreferencesKey("user")
    private val authUsersKey = stringPreferencesKey("auth-users")
    private val currentUserKey = stringPreferencesKey("current-user")
    private val colorSchemeKey = stringPreferencesKey("color-scheme")
    private val soundsEnabledKey = booleanPreferencesKey("sounds-enabled")
    private val gameRecordsKey = stringPreferencesKey("game-records")
    private val puzzleRatingRangeStartKey = intPreferencesKey("rating-range-start")
    private val puzzleRatingRangeEndKey = intPreferencesKey("rating-range-end")

    override suspend fun setHost(host: String) {
        store.edit { it[hostKey] = host }
    }

    override suspend fun host(): String = store.data.first()[hostKey] ?: ""

    override suspend fun clickToMove(): Boolean {
        return store.data.first()[clickToMove] ?: false
    }

    override suspend fun setClickToMove(enabled: Boolean) {
        store.edit { it[clickToMove] = enabled }
    }

    override suspend fun storeToken(token: String?) {
        store.edit { prefs ->
            token?.let { prefs[tokenKey] = token } ?: run { prefs.remove(tokenKey) }
        }
    }

    override suspend fun storeUserId(userId: UserId?) {
        store.edit { prefs ->
            userId?.let { prefs[userKey] = userId } ?: run { prefs.remove(userKey) }
        }
    }

    override suspend fun userId(): UserId? {
        return store.data.first()[userKey]
    }

    override suspend fun token(): String? {
        return store.data.first()[tokenKey]
    }

    override suspend fun colorSchemeUpdates(): Flow<String?> {
        return store.data.map { it[colorSchemeKey] }
    }

    override suspend fun colorScheme(): String? {
        return store.data.first()[colorSchemeKey]
    }

    override suspend fun setColorScheme(colorScheme: String) {
        store.edit { it[colorSchemeKey] = colorScheme }
    }

    override suspend fun setSoundsEnabled(enabled: Boolean) {
        store.edit { it[soundsEnabledKey] = enabled }
    }

    override suspend fun soundsEnabled(): Boolean {
        return store.data.first()[soundsEnabledKey] ?: false
    }

    override suspend fun setPuzzleRatingRange(range: IntRange) {
        store.edit {
            it[puzzleRatingRangeStartKey] = range.first
            it[puzzleRatingRangeEndKey] = range.last
        }
    }

    override suspend fun puzzleRatingRange(): IntRange {
        return store.data.first().let {
            val start = it[puzzleRatingRangeStartKey] ?: 0
            val end = it[puzzleRatingRangeEndKey] ?: 0
            start..end
        }
    }
    override suspend fun authUsers(): String? {
        return store.data.first()[authUsersKey]
    }

    override suspend fun authUsersUpdates(): Flow<String?> {
        return store.data.map { it[authUsersKey] }
    }

    override suspend fun setAuthUsers(value: String) {
        store.edit { it[authUsersKey] = value }
    }

    override suspend fun currentUser(): String? {
        return store.data.first()[currentUserKey]
    }

    override suspend fun currentUserUpdates(): Flow<String?> {
        return store.data.map { it[currentUserKey] }
    }

    override suspend fun setCurrentUser(username: String?) {
        store.edit { prefs ->
            username?.let { prefs[currentUserKey] = username } ?: run { prefs.remove(currentUserKey) }
        }
    }

    override suspend fun gameRecords(): String? {
        return store.data.first()[gameRecordsKey]
    }

    override suspend fun gameRecordsUpdates(): Flow<String?> {
        return store.data.map { it[gameRecordsKey] }
    }

    override suspend fun setGameRecords(value: String) {
        store.edit { it[gameRecordsKey] = value }
    }

    override suspend fun clear() {
        store.edit { it.clear() }
    }
}
