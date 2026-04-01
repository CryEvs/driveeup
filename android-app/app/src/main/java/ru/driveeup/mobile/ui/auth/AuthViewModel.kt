package ru.driveeup.mobile.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.driveeup.mobile.data.AuthRepository
import ru.driveeup.mobile.domain.User

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val user: User? = null,
    val token: String = "",
    val darkTheme: Boolean = false,
    val registerMode: Boolean = false
)

class AuthViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun restoreSession(token: String, darkTheme: Boolean) {
        _uiState.value = _uiState.value.copy(token = token, darkTheme = darkTheme)
        if (token.isNotBlank()) {
            refreshMe(token)
        }
    }

    fun setRegisterMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(registerMode = enabled, error = null)
    }

    fun setTheme(isDark: Boolean) {
        _uiState.value = _uiState.value.copy(darkTheme = isDark)
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Заполни все поля")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            runCatching { repo.login(email, password) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        token = it.accessToken,
                        user = it.user,
                        error = null
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(loading = false, error = it.message ?: "Login failed") }
        }
    }

    fun register(firstName: String, lastName: String, email: String, password: String, confirmPassword: String) {
        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Заполни все поля")
            return
        }
        if (password != confirmPassword) {
            _uiState.value = _uiState.value.copy(error = "Пароли не совпадают")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            runCatching { repo.register(firstName, lastName, email, password) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        token = it.accessToken,
                        user = it.user,
                        error = null,
                        registerMode = false
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(loading = false, error = it.message ?: "Register failed") }
        }
    }

    fun refreshMe(token: String = _uiState.value.token) {
        if (token.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.me(token) }
                .onSuccess { user -> _uiState.value = _uiState.value.copy(user = user, token = token, error = null) }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        token = "",
                        user = null,
                        error = "Сессия истекла, войди снова"
                    )
                }
        }
    }

    fun updateAvatar(avatarUrl: String) {
        val token = _uiState.value.token
        if (token.isBlank()) return
        if (avatarUrl.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Укажи ссылку на аватар")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            runCatching { repo.updateAvatar(token, avatarUrl) }
                .onSuccess { user -> _uiState.value = _uiState.value.copy(loading = false, user = user, error = null) }
                .onFailure { _uiState.value = _uiState.value.copy(loading = false, error = it.message ?: "Ошибка обновления аватара") }
        }
    }

    fun logout() {
        _uiState.value = _uiState.value.copy(token = "", user = null, error = null, registerMode = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
