package com.example.urisis_android.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
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
    val currentUser: User? = null,
    /**
     * True once the initial DataStore restore has completed, so the
     * splash screen can route correctly without racing the disk read.
     */
    val isInitialized: Boolean = false,
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val userDao = AppDatabase.get(application).userDao()
    private val accounts = AccountStore(application.applicationContext)

    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui.asStateFlow()

    val storedAccounts: StateFlow<List<AccountStore.StoredAccount>> =
        accounts.accounts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    init {
        viewModelScope.launch {
            val activeEmail = accounts.activeEmail.first()
            val user = activeEmail?.let { userDao.findByEmail(it) }
            _ui.update { it.copy(currentUser = user, isInitialized = true) }
        }
    }

    fun register(
        fullName: String,
        username: String,
        email: String,
        password: String,
        confirmPassword: String,
    ) {
        viewModelScope.launch {
            _ui.update { it.copy(status = AuthResult.Loading) }

            val nameTrim     = fullName.trim()
            val usernameTrim = username.trim().lowercase()
            val emailTrim    = email.trim().lowercase()

            val err = validate(nameTrim, usernameTrim, emailTrim, password, confirmPassword)
            if (err != null) {
                _ui.update { it.copy(status = AuthResult.Error(err)) }
                return@launch
            }
            if (userDao.findByEmail(emailTrim) != null) {
                _ui.update { it.copy(status = AuthResult.Error("An account with this email already exists")) }
                return@launch
            }
            if (userDao.findByUsername(usernameTrim) != null) {
                _ui.update { it.copy(status = AuthResult.Error("That username is already taken")) }
                return@launch
            }

            val salt = PasswordHasher.newSalt()
            val hash = PasswordHasher.hash(password, salt)
            val user = User(
                email = emailTrim,
                username = usernameTrim,
                fullName = nameTrim,
                passwordHash = hash,
                passwordSalt = salt
            )
            runCatching { userDao.insert(user) }
                .onSuccess {
                    accounts.addAndActivate(user.email, user.fullName)
                    _ui.update {
                        it.copy(status = AuthResult.Success(user), currentUser = user)
                    }
                }
                .onFailure {
                    _ui.update { it.copy(status = AuthResult.Error("Could not create account")) }
                }
        }
    }

    /** Authenticate using either an email address *or* a username. */
    fun login(identifier: String, password: String) {
        viewModelScope.launch {
            _ui.update { it.copy(status = AuthResult.Loading) }

            val idTrim = identifier.trim().lowercase()
            if (idTrim.isBlank() || password.isBlank()) {
                _ui.update { it.copy(status = AuthResult.Error("Enter your email/username and password")) }
                return@launch
            }

            val user = userDao.findByEmailOrUsername(idTrim)
            if (user == null || !PasswordHasher.verify(password, user.passwordSalt, user.passwordHash)) {
                _ui.update { it.copy(status = AuthResult.Error("Invalid credentials")) }
                return@launch
            }

            accounts.addAndActivate(user.email, user.fullName)
            _ui.update {
                it.copy(status = AuthResult.Success(user), currentUser = user)
            }
        }
    }

    fun switchAccount(email: String) {
        viewModelScope.launch {
            val user = userDao.findByEmail(email) ?: run {
                accounts.remove(email)
                _ui.update {
                    it.copy(status = AuthResult.Error("That account is no longer available"))
                }
                return@launch
            }
            accounts.switchTo(email)
            _ui.update { it.copy(currentUser = user, status = AuthResult.Idle) }
        }
    }

    fun removeAccount(email: String) {
        viewModelScope.launch {
            accounts.remove(email)
            if (_ui.value.currentUser?.email == email) {
                _ui.update { it.copy(currentUser = null) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            accounts.deactivate()
            _ui.update { it.copy(currentUser = null, status = AuthResult.Idle) }
        }
    }

    fun consumeResult() {
        _ui.update { it.copy(status = AuthResult.Idle) }
    }

    private fun validate(
        name: String,
        username: String,
        email: String,
        password: String,
        confirm: String,
    ): String? {
        val usernameRegex = Regex("^[a-z0-9._]{3,20}$")
        return when {
            name.isBlank() ->
                "Please enter your full name"
            !usernameRegex.matches(username) ->
                "Username must be 3-20 characters (letters, numbers, . or _)"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                "Enter a valid email address"
            password.length < 8 ->
                "Password must be at least 8 characters"
            !password.any { it.isUpperCase() } ->
                "Password must contain at least one uppercase letter"
            !password.any { it.isDigit() } ->
                "Password must contain at least one number"
            !password.any { !it.isLetterOrDigit() } ->
                "Password must contain at least one special character"
            password != confirm ->
                "Passwords do not match"
            else -> null
        }
    }
}