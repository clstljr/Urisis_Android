package com.example.urisis_android.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AuthResult {
    data object Idle : AuthResult
    data object Loading : AuthResult
    data class Success(val user: User) : AuthResult
    data class Error(val message: String) : AuthResult
}

data class AuthUiState(
    val status: AuthResult = AuthResult.Idle,
    val currentUser: User? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val userDao = AppDatabase.get(application).userDao()
    private val session = UserSession(application.applicationContext)

    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui.asStateFlow()

    init {
        // Restore session on app launch
        viewModelScope.launch {
            session.current.collect { saved ->
                if (saved != null) {
                    val user = userDao.findByEmail(saved.email)
                    if (user != null) _ui.update { it.copy(currentUser = user) }
                }
            }
        }
    }

    fun register(fullName: String, email: String, password: String, confirmPassword: String) {
        viewModelScope.launch {
            _ui.update { it.copy(status = AuthResult.Loading) }

            val nameTrim = fullName.trim()
            val emailTrim = email.trim().lowercase()

            val err = validate(nameTrim, emailTrim, password, confirmPassword)
            if (err != null) {
                _ui.update { it.copy(status = AuthResult.Error(err)) }
                return@launch
            }
            if (userDao.findByEmail(emailTrim) != null) {
                _ui.update { it.copy(status = AuthResult.Error("An account with this email already exists")) }
                return@launch
            }

            val salt = PasswordHasher.newSalt()
            val hash = PasswordHasher.hash(password, salt)
            val user = User(
                email = emailTrim,
                fullName = nameTrim,
                passwordHash = hash,
                passwordSalt = salt
            )
            runCatching { userDao.insert(user) }
                .onSuccess {
                    session.save(user.email, user.fullName)
                    _ui.update { it.copy(status = AuthResult.Success(user), currentUser = user) }
                }
                .onFailure {
                    _ui.update { it.copy(status = AuthResult.Error("Could not create account")) }
                }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _ui.update { it.copy(status = AuthResult.Loading) }

            val emailTrim = email.trim().lowercase()
            if (emailTrim.isBlank() || password.isBlank()) {
                _ui.update { it.copy(status = AuthResult.Error("Enter email and password")) }
                return@launch
            }

            val user = userDao.findByEmail(emailTrim)
            if (user == null || !PasswordHasher.verify(password, user.passwordSalt, user.passwordHash)) {
                _ui.update { it.copy(status = AuthResult.Error("Invalid email or password")) }
                return@launch
            }

            session.save(user.email, user.fullName)
            _ui.update { it.copy(status = AuthResult.Success(user), currentUser = user) }
        }
    }

    fun logout() {
        viewModelScope.launch {
            session.clear()
            _ui.value = AuthUiState()   // reset everything, including currentUser
        }
    }

    /** Call from the screen after handling the success/error state. */
    fun consumeResult() {
        _ui.update { it.copy(status = AuthResult.Idle) }
    }

    private fun validate(
        name: String, email: String, password: String, confirm: String
    ): String? = when {
        name.isBlank() -> "Please enter your full name"
        !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Enter a valid email"
        password.length < 6 -> "Password must be at least 6 characters"
        password != confirm -> "Passwords do not match"
        else -> null
    }
}
