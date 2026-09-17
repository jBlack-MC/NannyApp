package com.nannyapp.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.domain.model.User
import com.nannyapp.domain.model.UserRole
import com.nannyapp.domain.repository.AdminRepository
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUsersUiState(
    val loading: Boolean = true, val error: String? = null, val users: List<User> = emptyList(),
    val query: String = "", val roleFilter: UserRole? = null,
    val confirmingUserId: Int? = null, val confirmingSuspend: Boolean = false,
)

@HiltViewModel
class AdminUsersViewModel @Inject constructor(private val repository: AdminRepository) : ViewModel() {
    private val _state = MutableStateFlow(AdminUsersUiState())
    val state: StateFlow<AdminUsersUiState> = _state.asStateFlow()

    init { load() }

    fun onQueryChange(q: String) { _state.value = _state.value.copy(query = q); load() }
    fun setRoleFilter(role: UserRole?) { _state.value = _state.value.copy(roleFilter = role); load() }

    fun load() {
        viewModelScope.launch {
            repository.getUsers(_state.value.roleFilter, _state.value.query).collect { result ->
                _state.value = when (result) {
                    Resource.Loading -> _state.value.copy(loading = true, error = null)
                    is Resource.Success -> _state.value.copy(loading = false, users = result.data)
                    is Resource.Error -> _state.value.copy(loading = false, error = result.message)
                }
            }
        }
    }

    // request #25: no destructive action without confirmation
    fun requestSuspend(userId: Int) { _state.value = _state.value.copy(confirmingUserId = userId, confirmingSuspend = true) }
    fun requestActivate(userId: Int) { _state.value = _state.value.copy(confirmingUserId = userId, confirmingSuspend = false) }
    fun dismissConfirm() { _state.value = _state.value.copy(confirmingUserId = null) }

    fun confirmAction() {
        val userId = _state.value.confirmingUserId ?: return
        val suspend = _state.value.confirmingSuspend
        viewModelScope.launch {
            if (suspend) repository.suspendUser(userId) else repository.activateUser(userId)
            _state.value = _state.value.copy(confirmingUserId = null)
            load()
        }
    }
}
