package dev.mcd.chess.ui.screen.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mcd.chess.feature.auth.domain.LocalAuthRepository
import dev.mcd.chess.feature.auth.domain.UserRole
import dev.mcd.chess.feature.history.domain.GameRecord
import dev.mcd.chess.feature.history.domain.GameRecordRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val authRepository: LocalAuthRepository,
    private val gameRecordRepository: GameRecordRepository,
) : ViewModel(), ContainerHost<AdminViewModel.State, AdminViewModel.SideEffect> {

    override val container = container<State, SideEffect>(State()) {
        viewModelScope.launch {
            authRepository.sessionUpdates().collectLatest { session ->
                intent {
                    reduce { state.copy(isAdmin = session?.role == UserRole.Admin) }
                }
            }
        }
        viewModelScope.launch {
            gameRecordRepository.recordsUpdates().collectLatest { records ->
                intent {
                    reduce { state.copy(records = records) }
                }
            }
        }
    }

    data class State(
        val isAdmin: Boolean = false,
        val records: List<GameRecord> = emptyList(),
    )

    object SideEffect
}
