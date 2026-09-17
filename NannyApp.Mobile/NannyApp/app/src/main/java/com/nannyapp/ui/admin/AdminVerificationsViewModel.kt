package com.nannyapp.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.domain.model.NannyProfile
import com.nannyapp.domain.repository.AdminRepository
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminVerificationsUiState(val loading: Boolean = true, val error: String? = null, val pending: List<NannyProfile> = emptyList())

@HiltViewModel
class AdminVerificationsViewModel @Inject constructor(private val repository: AdminRepository) : ViewModel() {
    private val _state = MutableStateFlow(AdminVerificationsUiState())
    val state: StateFlow<AdminVerificationsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            repository.getPendingVerifications().collect { result ->
                _state.value = when (result) {
                    Resource.Loading -> _state.value.copy(loading = true, error = null)
                    is Resource.Success -> _state.value.copy(loading = false, pending = result.data)
                    is Resource.Error -> _state.value.copy(loading = false, error = result.message)
                }
            }
        }
    }

    fun approve(nannyId: Int, notes: String?) { viewModelScope.launch { repository.approveNanny(nannyId, notes); load() } }
    fun reject(nannyId: Int, notes: String?) { viewModelScope.launch { repository.rejectNanny(nannyId, notes); load() } }
}
