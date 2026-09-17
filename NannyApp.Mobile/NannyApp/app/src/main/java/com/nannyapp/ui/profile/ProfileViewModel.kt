package com.nannyapp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.domain.model.Gender
import com.nannyapp.domain.model.User
import com.nannyapp.domain.repository.AuthRepository
import com.nannyapp.domain.repository.UserRepository
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val user: User? = null,
    val completion: Int = 0,
    val editing: Boolean = false,
    val fullName: String = "",
    val phone: String = "",
    val dateOfBirth: String = "",
    val address: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val loggedOut: Boolean = false,
    // password change
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val passwordError: String? = null,
    val passwordChanged: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            when (val result = userRepository.getProfile()) {
                is Resource.Success -> {
                    val u = result.data
                    _state.value = _state.value.copy(
                        loading = false, user = u, completion = userRepository.profileCompletionPercent(u),
                        fullName = u.fullName, phone = u.phone ?: "", dateOfBirth = u.dateOfBirth ?: "", address = u.address ?: "",
                    )
                }
                is Resource.Error -> _state.value = _state.value.copy(loading = false, error = result.message)
                Resource.Loading -> {}
            }
        }
    }

    fun toggleEdit(editing: Boolean) { _state.value = _state.value.copy(editing = editing, saved = false) }
    fun update(transform: (ProfileUiState) -> ProfileUiState) { _state.value = transform(_state.value) }

    fun save() {
        val u = _state.value.user ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true)
            val updated = u.copy(fullName = _state.value.fullName, phone = _state.value.phone, dateOfBirth = _state.value.dateOfBirth, address = _state.value.address)
            when (val result = userRepository.updateProfile(updated)) {
                is Resource.Success -> _state.value = _state.value.copy(saving = false, saved = true, editing = false, user = result.data, completion = userRepository.profileCompletionPercent(result.data))
                is Resource.Error -> _state.value = _state.value.copy(saving = false, error = result.message)
                Resource.Loading -> {}
            }
        }
    }

    fun changePassword() {
        val s = _state.value
        if (s.newPassword.length < 8) { _state.value = s.copy(passwordError = "New password must be at least 8 characters."); return }
        if (s.newPassword != s.confirmNewPassword) { _state.value = s.copy(passwordError = "Passwords do not match."); return }
        viewModelScope.launch {
            _state.value = _state.value.copy(passwordError = null)
            when (val result = userRepository.changePassword(s.currentPassword, s.newPassword)) {
                is Resource.Success -> _state.value = _state.value.copy(passwordChanged = true, currentPassword = "", newPassword = "", confirmNewPassword = "")
                is Resource.Error -> _state.value = _state.value.copy(passwordError = result.message)
                Resource.Loading -> {}
            }
        }
    }

    fun uploadAvatar(bytes: ByteArray, fileName: String) {
        viewModelScope.launch {
            when (val result = userRepository.uploadProfileImage(bytes, fileName)) {
                is Resource.Success -> load() // refresh from server so profileImageUrl is authoritative
                is Resource.Error -> _state.value = _state.value.copy(error = result.message)
                Resource.Loading -> {}
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _state.value = _state.value.copy(loggedOut = true)
        }
    }
}
