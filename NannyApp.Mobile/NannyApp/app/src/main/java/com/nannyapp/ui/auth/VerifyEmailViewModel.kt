package com.nannyapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.domain.repository.AuthRepository
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VerifyEmailUiState(
    val token: String = "",
    val email: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val verified: Boolean = false,
    val resendLoading: Boolean = false,
    val resendSent: Boolean = false,
)

/** Backs request #6's "Unverified email" case and the standalone verification
 * link (auth/verify-email.php equivalent): the person either taps a link that
 * deep-links here with the token pre-filled, or pastes it in manually, and can
 * also request a fresh link if theirs expired. */
@HiltViewModel
class VerifyEmailViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(VerifyEmailUiState())
    val state: StateFlow<VerifyEmailUiState> = _state.asStateFlow()

    fun setInitialToken(token: String?) {
        if (!token.isNullOrBlank()) _state.value = _state.value.copy(token = token)
    }

    fun onTokenChange(v: String) { _state.value = _state.value.copy(token = v, error = null) }
    fun onEmailChange(v: String) { _state.value = _state.value.copy(email = v, error = null) }

    fun verify() {
        val token = _state.value.token.trim()
        if (token.isEmpty()) {
            _state.value = _state.value.copy(error = "Please enter the verification code from your email.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            when (val result = authRepository.verifyEmail(token)) {
                is Resource.Success -> _state.value = _state.value.copy(loading = false, verified = true)
                is Resource.Error -> _state.value = _state.value.copy(loading = false, error = result.message)
                Resource.Loading -> {}
            }
        }
    }

    fun resend() {
        val email = _state.value.email.trim()
        if (!email.contains("@")) {
            _state.value = _state.value.copy(error = "Enter your email above first so we know where to send it.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(resendLoading = true, error = null, resendSent = false)
            when (val result = authRepository.resendVerification(email)) {
                is Resource.Success -> _state.value = _state.value.copy(resendLoading = false, resendSent = true)
                is Resource.Error -> _state.value = _state.value.copy(resendLoading = false, error = result.message)
                Resource.Loading -> {}
            }
        }
    }
}
