package dev.mcd.chess.ui.screen.choosemode

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mcd.chess.common.game.GameId
import dev.mcd.chess.online.domain.usecase.GetGameForUser
import dev.mcd.chess.online.domain.usecase.GetLobbyInfo
import kotlinx.coroutines.delay
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.syntax.simple.repeatOnSubscription
import org.orbitmvi.orbit.viewmodel.container
import timber.log.Timber
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import dev.mcd.chess.feature.auth.domain.LocalAuthRepository
import dev.mcd.chess.feature.auth.domain.UserRole
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltViewModel
class ChooseModeViewModel @Inject constructor(
    private val getGameForUser: GetGameForUser,
    private val getLobbyInfo: GetLobbyInfo,
    private val authRepository: LocalAuthRepository,
) : ViewModel(), ContainerHost<ChooseModeViewModel.State, ChooseModeViewModel.SideEffect> {

    override val container = container<State, SideEffect>(State()) {
        viewModelScope.launch {
            authRepository.sessionUpdates().collectLatest { session ->
                intent {
                    reduce {
                        state.copy(
                            username = session?.username,
                            isAdmin = session?.role == UserRole.Admin,
                        )
                    }
                }
            }
        }
        intent {
            repeatOnSubscription {
                runCatching {
                    val existingGameId = getGameForUser()
                    if (existingGameId != null) {
                        postSideEffect(SideEffect.NavigateToExistingGame(existingGameId))
                    }
                }.onFailure {
                    Timber.e(it, "Finding existing games")
                }

                while (true) {
                    runCatching {
                        val lobbyInfo = getLobbyInfo()
                        reduce { state.copy(inLobby = lobbyInfo.inLobby) }
                    }.onFailure {
                        Timber.e(it, "Getting lobby info")
                    }
                    delay(2000)
                }
            }
        }
    }

    data class State(
        val inLobby: Int? = null,
        val username: String? = null,
        val isAdmin: Boolean = false,
    )
    sealed interface SideEffect {
        data class NavigateToExistingGame(val id: GameId) : SideEffect
    }
}
