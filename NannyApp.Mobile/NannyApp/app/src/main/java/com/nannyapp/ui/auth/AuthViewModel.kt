package com.nannyapp.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.nannyapp.BuildConfig
import com.nannyapp.data.preferences.SessionManager
import com.nannyapp.domain.model.Gender
import com.nannyapp.domain.model.UserRole
import com.nannyapp.domain.repository.AuthRepository
import com.nannyapp.domain.repository.RegisterData
import com.nannyapp.domain.repository.UserRepository
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SessionCheck {
    data object Checking : SessionCheck()
    data class LoggedIn(val role: UserRole) : SessionCheck()
    data object LoggedOut : SessionCheck()
    data class Retry(val message: String) : SessionCheck()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val userRepository: UserRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<SessionCheck>(SessionCheck.Checking)
    val state: StateFlow<SessionCheck> = _state.asStateFlow()

    init {
        retry()
    }

    fun retry() {
        viewModelScope.launch {
            _state.value = SessionCheck.Checking
            val role = sessionManager.restoreSessionRole()
            // Older app installs may still have an admin session stored locally.
            // Remove it rather than exposing a native administration route.
            if (role == UserRole.ADMIN) {
                sessionManager.clearSession()
                _state.value = SessionCheck.LoggedOut
            } else if (role == null) {
                _state.value = SessionCheck.LoggedOut
            } else {
                val token = sessionManager.currentToken()
                when (val result = userRepository.getProfile(allowOfflineCache = false)) {
                    is Resource.Success -> {
                        if (token == null || sessionManager.currentToken() != token) {
                            _state.value = SessionCheck.LoggedOut
                        } else if (result.data.role == UserRole.ADMIN) {
                            sessionManager.clearSessionIfCurrent(token)
                            _state.value = SessionCheck.LoggedOut
                        } else {
                            _state.value = SessionCheck.LoggedIn(result.data.role)
                        }
                    }
                    is Resource.Error -> {
                        _state.value = if (sessionManager.currentToken() == null) SessionCheck.LoggedOut
                            else SessionCheck.Retry(result.message)
                    }
                    Resource.Loading -> _state.value = SessionCheck.Retry("Couldn't check your session. Please try again.")
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            sessionManager.clearSession()
            _state.value = SessionCheck.LoggedOut
        }
    }
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val rememberMe: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val loggedInRole: UserRole? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(v: String) { _state.value = _state.value.copy(email = v, error = null) }
    fun onPasswordChange(v: String) { _state.value = _state.value.copy(password = v, error = null) }
    fun onRememberMeChange(v: Boolean) { _state.value = _state.value.copy(rememberMe = v) }

    fun login() {
        val s = _state.value
        if (s.email.isBlank() || s.password.isBlank()) {
            _state.value = s.copy(error = "Please enter your email and password.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            when (val result = authRepository.login(s.email.trim(), s.password, s.rememberMe)) {
                is Resource.Success -> _state.value = _state.value.copy(loading = false, loggedInRole = result.data.role)
                is Resource.Error -> _state.value = _state.value.copy(loading = false, error = friendlyLoginError(result.message))
                Resource.Loading -> {}
            }
        }
    }

    fun googleSso(context: Context) {
        val rememberMe = _state.value.rememberMe
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                    .build()
                val request = GetCredentialRequest(listOf(googleIdOption))
                val response = credentialManager.getCredential(context = context, request = request)
                val googleCred = GoogleIdTokenCredential.createFrom(response.credential.data)
                when (val result = authRepository.googleLogin(googleCred.idToken, rememberMe)) {
                    is Resource.Success -> _state.value = _state.value.copy(loading = false, loggedInRole = result.data.role)
                    is Resource.Error -> _state.value = _state.value.copy(loading = false, error = result.message)
                    Resource.Loading -> {}
                }
            } catch (e: GetCredentialCancellationException) {
                _state.value = _state.value.copy(loading = false, error = null)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = "Google sign-in failed. Please try again.")
            }
        }
    }

    /** Maps generic backend messages onto the specific cases called out in request #6:
     * invalid credentials, suspended account, unverified email, network errors. */
    private fun friendlyLoginError(message: String): String = when {
        message.contains("suspend", ignoreCase = true) -> "Your account has been suspended. Contact support for help."
        message.contains("verif", ignoreCase = true) -> "Please verify your email before logging in. Check your inbox for the verification link."
        message.contains("credential", ignoreCase = true) || message.contains("password", ignoreCase = true) || message.contains("Invalid", ignoreCase = true) ->
            "Incorrect email or password. Please try again."
        else -> message
    }
}

data class RegisterUiState(
    val role: UserRole = UserRole.PARENT,
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val dateOfBirth: String = "",
    val address: String = "",
    val gender: Gender = Gender.UNSPECIFIED,
    val emergencyContactName: String = "",
    val emergencyContact: String = "",
    val emergencyContactRelationship: String = "",
    val numberOfChildren: String = "",
    val bio: String = "",
    val experienceYears: String = "",
    val hourlyRate: String = "",
    val location: String = "",
    val skills: String = "",
    val languages: String = "",
    val qualifications: String = "",
    val specialisations: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val registered: Boolean = false,
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun update(transform: (RegisterUiState) -> RegisterUiState) {
        _state.value = transform(_state.value).copy(error = null)
    }

    private fun validate(s: RegisterUiState): String? {
        if (s.fullName.isBlank()) return "Please enter your full name."
        if (!s.email.contains("@")) return "Please enter a valid email address."
        if (s.password.length < 8) return "Password must be at least 8 characters."
        if (s.password != s.confirmPassword) return "Passwords do not match."
        if (s.role == UserRole.NANNY && s.hourlyRate.isNotBlank() &&
            (s.hourlyRate.toDoubleOrNull()?.let { !it.isFinite() || it <= 0.0 } ?: true)) {
            return "Please enter a valid hourly rate."
        }
        return null
    }

    fun register() {
        val s = _state.value
        val validationError = validate(s)
        if (validationError != null) {
            _state.value = s.copy(error = validationError)
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val data = RegisterData(
                role = s.role, fullName = s.fullName.trim(), email = s.email.trim(), phone = s.phone.trim().ifBlank { null },
                password = s.password, dateOfBirth = s.dateOfBirth.ifBlank { null }, address = s.address.ifBlank { null },
                gender = s.gender,
                emergencyContact = s.emergencyContact.ifBlank { null },
                emergencyContactName = s.emergencyContactName.ifBlank { null },
                emergencyContactRelationship = s.emergencyContactRelationship.ifBlank { null },
                numberOfChildren = s.numberOfChildren.toIntOrNull(),
                bio = s.bio.ifBlank { null },
                experienceYears = s.experienceYears.toIntOrNull(),
                hourlyRate = s.hourlyRate.toDoubleOrNull(),
                location = s.location.ifBlank { null },
                skills = s.skills.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                languages = s.languages.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                qualifications = s.qualifications.ifBlank { null },
                specialisations = s.specialisations.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            )
            when (val result = authRepository.register(data)) {
                is Resource.Success -> _state.value = _state.value.copy(loading = false, registered = true)
                is Resource.Error -> _state.value = _state.value.copy(loading = false, error = result.message)
                Resource.Loading -> {}
            }
        }
    }
}

data class ForgotPasswordUiState(
    val email: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val sent: Boolean = false,
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    fun onEmailChange(v: String) { _state.value = _state.value.copy(email = v, error = null) }

    fun submit() {
        val email = _state.value.email
        if (!email.contains("@")) { _state.value = _state.value.copy(error = "Please enter a valid email."); return }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            when (val result = authRepository.forgotPassword(email.trim())) {
                is Resource.Success -> _state.value = _state.value.copy(loading = false, sent = true)
                is Resource.Error -> _state.value = _state.value.copy(loading = false, error = result.message)
                Resource.Loading -> {}
            }
        }
    }
}
