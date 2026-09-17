package com.nannyapp.ui.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.domain.model.SupportCategory
import com.nannyapp.domain.model.SupportTicket
import com.nannyapp.domain.repository.SupportRepository
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupportUiState(val loading: Boolean = true, val error: String? = null, val tickets: List<SupportTicket> = emptyList())

@HiltViewModel
class SupportViewModel @Inject constructor(private val repository: SupportRepository) : ViewModel() {
    private val _state = MutableStateFlow(SupportUiState())
    val state: StateFlow<SupportUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            repository.getMyTickets().collect { result ->
                _state.value = when (result) {
                    Resource.Loading -> _state.value.copy(loading = true, error = null)
                    is Resource.Success -> _state.value.copy(loading = false, tickets = result.data)
                    is Resource.Error -> _state.value.copy(loading = false, error = result.message)
                }
            }
        }
    }
}

data class NewTicketUiState(
    val category: SupportCategory = SupportCategory.GENERAL,
    val subject: String = "",
    val message: String = "",
    val name: String = "",
    val email: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val submitted: Boolean = false,
)

@HiltViewModel
class NewTicketViewModel @Inject constructor(private val repository: SupportRepository) : ViewModel() {
    private val _state = MutableStateFlow(NewTicketUiState())
    val state: StateFlow<NewTicketUiState> = _state.asStateFlow()

    fun update(transform: (NewTicketUiState) -> NewTicketUiState) { _state.value = transform(_state.value).copy(error = null) }

    fun submit() {
        val s = _state.value
        if (s.subject.isBlank() || s.message.isBlank()) { _state.value = s.copy(error = "Please fill in the subject and message."); return }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            when (val result = repository.submitTicket(s.category, s.subject, s.message, s.name, s.email)) {
                is Resource.Success -> _state.value = _state.value.copy(loading = false, submitted = true)
                is Resource.Error -> _state.value = _state.value.copy(loading = false, error = result.message)
                Resource.Loading -> {}
            }
        }
    }
}
