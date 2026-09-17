package com.nannyapp.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.domain.model.Booking
import com.nannyapp.domain.model.BookingStatus
import com.nannyapp.domain.repository.AdminRepository
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminBookingsUiState(
    val loading: Boolean = true, val error: String? = null, val bookings: List<Booking> = emptyList(),
    val statusFilter: BookingStatus? = null, val query: String = "",
)

@HiltViewModel
class AdminBookingsViewModel @Inject constructor(private val repository: AdminRepository) : ViewModel() {
    private val _state = MutableStateFlow(AdminBookingsUiState())
    val state: StateFlow<AdminBookingsUiState> = _state.asStateFlow()

    init { load() }

    fun setFilter(status: BookingStatus?) { _state.value = _state.value.copy(statusFilter = status); load() }
    fun onQueryChange(q: String) { _state.value = _state.value.copy(query = q); load() }

    fun load() {
        viewModelScope.launch {
            repository.getAllBookings(_state.value.statusFilter, _state.value.query).collect { result ->
                _state.value = when (result) {
                    Resource.Loading -> _state.value.copy(loading = true, error = null)
                    is Resource.Success -> _state.value.copy(loading = false, bookings = result.data)
                    is Resource.Error -> _state.value.copy(loading = false, error = result.message)
                }
            }
        }
    }

    fun updateStatus(bookingId: Int, status: BookingStatus) {
        viewModelScope.launch { repository.updateBookingStatus(bookingId, status); load() }
    }
}
