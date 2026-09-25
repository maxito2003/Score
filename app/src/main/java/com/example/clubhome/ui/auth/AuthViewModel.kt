package com.example.clubhome.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clubhome.data.repository.AuthRepository
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val currentUser: UserInfo? = null,
    val isLoggedIn: Boolean = false
)

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        val user = authRepository.getCurrentUser()
        if (user != null) {
            _uiState.update {
                it.copy(
                    currentUser = user,
                    isLoggedIn = true
                )
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, errorMessage = null) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, errorMessage = null) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun signIn(onSuccess: () -> Unit) {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password

        if (email.isEmpty() || password.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Por favor, completa todos los campos.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val user = authRepository.signIn(email, password)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentUser = user,
                        isLoggedIn = true,
                        errorMessage = null
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = parseAuthErrorMessage(e)
                    )
                }
            }
        }
    }

    fun signUp(onSuccess: () -> Unit) {
        val name = _uiState.value.name.trim()
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password
        val confirmPassword = _uiState.value.confirmPassword

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Por favor, completa todos los campos.") }
            return
        }

        if (password != confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Las contraseñas no coinciden.") }
            return
        }

        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "La contraseña debe tener al menos 6 caracteres.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val user = authRepository.signUp(email, password, name)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentUser = user,
                        isLoggedIn = true, // Permite ingresar directamente al usuario
                        successMessage = "¡Cuenta creada exitosamente!"
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = parseAuthErrorMessage(e)
                    )
                }
            }
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            try {
                authRepository.signOut()
            } catch (_: Exception) {}
            _uiState.update {
                AuthUiState()
            }
            onSignedOut()
        }
    }

    private fun parseAuthErrorMessage(e: Exception): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("email_not_confirmed", ignoreCase = true) ->
                "Tu correo no ha sido confirmado. Para ingresar sin confirmar correo, desactiva la opción 'Confirm email' en tu panel de Supabase (Authentication -> Email)."
            msg.contains("Invalid login credentials", ignoreCase = true) || msg.contains("invalid_credentials", ignoreCase = true) ->
                "Correo o contraseña incorrectos. Verifica tus datos."
            msg.contains("User already registered", ignoreCase = true) || msg.contains("user_already_exists", ignoreCase = true) ->
                "Este correo ya está registrado. Intenta iniciar sesión."
            else -> "Ocurrió un error al procesar tu solicitud. Intenta nuevamente."
        }
    }
}
