package dev.mcd.chess.ui.screen.onlinegame

import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
//import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.move.Move
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mcd.chess.common.game.GameId
import dev.mcd.chess.common.game.MoveResult
import dev.mcd.chess.common.game.TerminationReason
import dev.mcd.chess.feature.game.domain.GameSessionRepository
import dev.mcd.chess.feature.share.domain.CopySessionPGNToClipboard
import dev.mcd.chess.feature.sound.domain.GameSessionSoundWrapper
import dev.mcd.chess.feature.sound.domain.SoundSettings
import dev.mcd.chess.online.domain.OnlineGameSession
import dev.mcd.chess.online.domain.usecase.FindGame
import dev.mcd.chess.online.domain.usecase.GetOrCreateUser
import dev.mcd.chess.online.domain.usecase.JoinOnlineGame
import dev.mcd.chess.online.domain.usecase.JoinOnlineGame.Event
import dev.mcd.chess.ui.screen.onlinegame.OnlineGameViewModel.SideEffect.AnnounceTermination
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import dev.mcd.chess.common.game.GameSession
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side
import dev.mcd.chess.common.player.HumanPlayer
import dev.mcd.chess.common.player.PlayerImage
import dev.mcd.chess.feature.auth.domain.LocalAuthRepository
import dev.mcd.chess.feature.history.domain.GameRecord
import dev.mcd.chess.feature.history.domain.GameRecordMode
import dev.mcd.chess.feature.history.domain.GameRecordRepository
import java.util.UUID



@HiltViewModel
class OnlineGameViewModel @Inject constructor(
    private val gameSessionRepository: GameSessionRepository,
    private val stateHandle: SavedStateHandle,
    private val joinOnlineGame: JoinOnlineGame,
    private val getOrCreateUser: GetOrCreateUser,
    private val findGame: FindGame,
    private val soundWrapper: GameSessionSoundWrapper,
    private val appPreferences: AppPreferences,

    private val copyPGN: CopySessionPGNToClipboard,
    private val gameRecordRepository: GameRecordRepository,
    private val authRepository: LocalAuthRepository,
) : ViewModel(), ContainerHost<OnlineGameViewModel.State, OnlineGameViewModel.SideEffect> {
    private val isLocalTwoPlayer: Boolean = true
    private val recordedSessions = mutableSetOf<String>()
    override val container = container<State, SideEffect>(
        initialState = State.FindingGame(),
    ) {
        viewModelScope.launch {
            gameSessionRepository.activeGame()
                .mapNotNull { it } // o directamente .filterNotNull() si es nullable
                .collectLatest { session ->
                    intent { reduce { State.InGame(session = session) } }
                    intent {
                        val settings = SoundSettings(
                            enabled = appPreferences.soundsEnabled(),
                            enableNotify = true,
                        )
                        soundWrapper.attachSession(session, settings)
                    }
                }

        }
        val gameId = stateHandle.get<String>("gameId")

        if (isLocalTwoPlayer) {
            intent {
                    startLocalTwoPlayerGame()

            }
        } else if (gameId != null) {
            intent {
                runCatching {
                    startGame(gameId)
                }.onFailure {
                    Timber.e(it, "Retrieving game $gameId")
                    fatalError("Unable to retrieve existing game")
                }
            }
        } else {
            findGame()
        }

    }

    fun onRestart() {
        findGame()
    }

    fun onResign(andNavigateBack: Boolean = false) {
        intent {
            gameSessionRepository.activeGame().firstOrNull()?.run {
                if (confirmResignation()) {
                    resign()
                }
            }
            if (andNavigateBack) {
                postSideEffect(SideEffect.NavigateBack)
            }
        }
    }

    fun onPlayerMove(move: Move) {
        intent {
            gameSessionRepository.activeGame().firstOrNull()?.run {
                if (move(move.toString()) == MoveResult.Moved) {
                    if (!isLocalTwoPlayer) {
                        clientSession()?.channel?.move(move)
                    }
                } else {
                    Timber.e("Illegal Move: $move")
                }
            }
        }
    }


    fun onCopyPGN() {
        intent {
            runCatching {
                val session = gameSessionRepository.activeGame().value ?: return@intent
                copyPGN(session)
                postSideEffect(SideEffect.NotifyGameCopied)
            }.onFailure {
                Timber.e(it, "copying PGN")
            }
        }
    }

    private suspend fun confirmResignation(): Boolean {
        return suspendCoroutine { continuation ->
            intent {
                postSideEffect(
                    SideEffect.ConfirmResignation(
                        onConfirm = { continuation.resume(true) },
                        onDismiss = { continuation.resume(false) },
                    ),
                )
            }
        }
    }

    private fun findGame() {
        intent {
            runCatching {
                val userId = getOrCreateUser()
                reduce { State.FindingGame(userId) }

                Timber.d("Authenticated as $userId")

                val id = findGame.invoke()
                startGame(id)
            }.onFailure {
                Timber.e(it, "findingGame")
            }
        }
    }

    private fun startGame(id: GameId) {
        intent {
            runCatching {
                joinOnlineGame(id).collectLatest { event ->
                    when (event) {
                        is Event.FatalError -> fatalError(event.message)
                        is Event.Termination -> handleTermination(event.reason)
                        is Event.NewSession -> gameSessionRepository.updateActiveGame(event.session)
                    }
                }
            }.onFailure {
                fatalError("joining game", it)
            }
        }
    }
    private fun startLocalTwoPlayerGame() {
        intent {
            runCatching {
                // Limpia cualquier partida anterior para que al volver siempre empiece una nueva
                gameSessionRepository.updateActiveGame(null)

                val white = HumanPlayer("Invitado 1", PlayerImage.Default, 0)
                val black = HumanPlayer("Invitado 2", PlayerImage.Default, 0)

                val session = GameSession(
                    id = "local-guest-${System.currentTimeMillis()}",
                    self = white,
                    selfSide = Side.WHITE,
                    opponent = black,
                )

                session.setBoard(Board())

                gameSessionRepository.updateActiveGame(session)
                // El collector de activeGame ya hará el reduce a InGame
            }.onFailure {
                Timber.e(it, "Starting local two-player game")
                fatalError("Unable to start local two-player game", it)
            }
        }
    }


    private fun handleTermination(reason: TerminationReason) {
        intent {
            recordGame(reason)
            postSideEffect(
                AnnounceTermination(
                    sideMated = reason.sideMated,
                    draw = reason.draw,
                    resignation = reason.resignation,
                ),
            )
        }
    }
    private suspend fun recordGame(reason: TerminationReason) {
        val session = gameSessionRepository.activeGame().value ?: return
        val recordId = session.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        if (!recordedSessions.add(recordId)) {
            return
        }
        val username = authRepository.session()?.username ?: "guest"
        val record = GameRecord(
            id = recordId,
            username = username,
            mode = GameRecordMode.Online,
            moves = session.history().map { it.move.toString() },
            result = reason.toSummary(),
            createdAt = System.currentTimeMillis(),
        )
        gameRecordRepository.addRecord(record)
    }

    private fun TerminationReason.toSummary(): String {
        return when {
            draw -> "Draw"
            resignation != null -> "Resignation"
            sideMated != null -> "Checkmate"
            else -> "Finished"
        }
    }

    private fun fatalError(message: String, throwable: Throwable? = null) {
        Timber.e(throwable, message)
        intent {
            reduce { State.FatalError(message) }
        }
    }

    private fun clientSession(): OnlineGameSession? =
        (container.stateFlow.value as? State.InGame)?.session as? OnlineGameSession

    sealed interface State {
        data class InGame(
            val session: GameSession,
        ) : State

        data class FindingGame(
            val username: String? = null,
        ) : State

        data class FatalError(
            val message: String,
        ) : State
    }

    sealed interface SideEffect {
        data class ConfirmResignation(
            val onConfirm: () -> Unit,
            val onDismiss: () -> Unit,
        ) : SideEffect

        @Stable
        data class AnnounceTermination(
            val sideMated: Side? = null,
            val draw: Boolean = false,
            val resignation: Side? = null,
        ) : SideEffect

        object NotifyGameCopied : SideEffect

        object NavigateBack : SideEffect
    }
}
