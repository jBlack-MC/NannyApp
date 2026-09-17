package com.nannyapp.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.domain.model.SupportStatus
import com.nannyapp.domain.model.SupportTicket
import com.nannyapp.domain.repository.AdminRepository
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminSupportUiState(val loading: Boolean = true, val error: String? = null, val tickets: List<SupportTicket> = emptyList(), val statusFilter: SupportStatus? = null)

@HiltViewModel
class AdminSupportViewModel @Inject constructor(private val repository: AdminRepository) : ViewModel() {
    private val _state = MutableStateFlow(AdminSupportUiState())
    val state: StateFlow<AdminSupportUiState> = _state.asStateFlow()

    init { load() }

    fun setFilter(status: SupportStatus?) { _state.value = _state.value.copy(statusFilter = status); load() }

    fun load() {
        viewModelScope.launch {
            repository.getAllSupportTickets(_state.value.statusFilter).collect { result ->
                _state.value = when (result) {
                    Resource.Loading -> _state.value.copy(loading = true, error = null)
                    is Resource.Success -> _state.value.copy(loading = false, tickets = result.data)
                    is Resource.Error -> _state.value.copy(loading = false, error = result.message)
                }
            }
        }
    }

    fun updateStatus(ticketId: Int, status: SupportStatus, notes: String?) {
        viewModelScope.launch { repository.updateTicketStatus(ticketId, status, notes); load() }
    }
}
