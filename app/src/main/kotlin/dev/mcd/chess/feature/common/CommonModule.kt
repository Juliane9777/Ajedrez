package dev.mcd.chess.feature.common

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.mcd.chess.BuildConfig
import dev.mcd.chess.feature.common.data.AppPreferencesImpl
import dev.mcd.chess.feature.common.data.TranslationsImpl
import dev.mcd.chess.feature.common.domain.Environment
import dev.mcd.chess.feature.common.domain.Translations
import dev.mcd.chess.feature.auth.data.LocalAuthRepositoryImpl
import dev.mcd.chess.feature.auth.domain.LocalAuthRepository
import dev.mcd.chess.feature.history.data.GameRecordRepositoryImpl
import dev.mcd.chess.feature.history.domain.GameRecordRepository
import dev.mcd.chess.online.domain.AuthStore
import dev.mcd.chess.online.domain.EndpointProvider
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CommonModule {

    @Binds
    @Singleton
    abstract fun appPrefs(impl: AppPreferencesImpl): AppPreferences

    @Binds
    @Singleton
    abstract fun translations(impl: TranslationsImpl): Translations
    @Binds
    @Singleton
    abstract fun localAuthRepository(impl: LocalAuthRepositoryImpl): LocalAuthRepository

    @Binds
    @Singleton
    abstract fun gameRecordRepository(impl: GameRecordRepositoryImpl): GameRecordRepository
    companion object {
        @Provides
        @Singleton
        fun environment(appPreferences: AppPreferences): Environment {
            return runBlocking {
                val apiUrl = if (BuildConfig.DEBUG && appPreferences.host().isNotBlank()) {
                    appPreferences.host()
                } else {
                    BuildConfig.ONLINE_API_HOST
                }
                Environment(apiUrl = apiUrl)
            }
        }

        @Provides
        @Singleton
        fun endpointProvider(environment: Environment): EndpointProvider = object : EndpointProvider {
            override fun invoke() = environment.apiUrl
        }

        @Provides
        @Singleton
        fun authStore(appPreferences: AppPreferences): AuthStore = appPreferences
    }
}
