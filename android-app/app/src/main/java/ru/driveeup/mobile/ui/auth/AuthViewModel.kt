package ru.driveeup.mobile.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.driveeup.mobile.data.AuthRepository
import ru.driveeup.mobile.domain.User
import ru.driveeup.mobile.domain.UserRole

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val user: User? = null
)

class AuthViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState(error = "Заполни все поля")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState(loading = true)
            runCatching { repo.login(email, password) }
                .onSuccess { _uiState.value = AuthUiState(user = it) }
                .onFailure { _uiState.value = AuthUiState(error = it.message ?: "Login failed") }
        }
    }

    fun register(email: String, password: String, role: UserRole) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState(error = "Заполни все поля")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState(loading = true)
            runCatching { repo.register(email, password, role) }
                .onSuccess { _uiState.value = AuthUiState(user = it) }
                .onFailure { _uiState.value = AuthUiState(error = it.message ?: "Register failed") }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
