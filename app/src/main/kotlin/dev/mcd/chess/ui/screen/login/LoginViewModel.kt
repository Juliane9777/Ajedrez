package dev.mcd.chess.ui.screen.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mcd.chess.feature.auth.domain.AuthResult
import dev.mcd.chess.feature.auth.domain.LocalAuthRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: LocalAuthRepository,
) : ViewModel(), ContainerHost<LoginViewModel.State, LoginViewModel.SideEffect> {

    override val container = container<State, SideEffect>(State()) {
        viewModelScope.launch {
            authRepository.sessionUpdates().collectLatest { session ->
                if (session != null) {
                    intent { postSideEffect(SideEffect.NavigateToHome) }
                }
            }
        }
    }

    fun onUsernameChanged(value: String) {
        intent { reduce { state.copy(username = value, error = null) } }
    }

    fun onPasswordChanged(value: String) {
        intent { reduce { state.copy(password = value, error = null) } }
    }

    fun onLogin() {
        intent {
            reduce { state.copy(loading = true, error = null) }
            when (val result = authRepository.login(state.username.trim(), state.password)) {
                is AuthResult.Success -> reduce { state.copy(loading = false) }
                is AuthResult.Error -> reduce { state.copy(loading = false, error = result.message) }
            }
        }
    }

    fun onRegister() {
        intent {
            reduce { state.copy(loading = true, error = null) }
            when (val result = authRepository.register(state.username.trim(), state.password)) {
                is AuthResult.Success -> reduce { state.copy(loading = false) }
                is AuthResult.Error -> reduce { state.copy(loading = false, error = result.message) }
            }
        }
    }

    data class State(
        val username: String = "",
        val password: String = "",
        val error: String? = null,
        val loading: Boolean = false,
    )

    sealed interface SideEffect {
        object NavigateToHome : SideEffect
    }
}
