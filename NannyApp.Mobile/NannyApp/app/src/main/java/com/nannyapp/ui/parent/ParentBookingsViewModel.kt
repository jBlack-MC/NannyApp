package com.nannyapp.ui.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.domain.model.Booking
import com.nannyapp.domain.model.BookingStatus
import com.nannyapp.domain.model.UserRole
import com.nannyapp.domain.repository.BookingRepository
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ParentBookingsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val bookings: List<Booking> = emptyList(),
    val filter: BookingStatus? = null,
)

@HiltViewModel
class ParentBookingsViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ParentBookingsUiState())
    val state: StateFlow<ParentBookingsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            bookingRepository.getBookings(UserRole.PARENT).collect { result ->
                _state.value = when (result) {
                    Resource.Loading -> _state.value.copy(loading = true, error = null)
                    is Resource.Success -> _state.value.copy(loading = false, bookings = result.data)
                    is Resource.Error -> _state.value.copy(loading = false, error = result.message)
                }
            }
        }
    }

    fun setFilter(status: BookingStatus?) { _state.value = _state.value.copy(filter = status) }

    val filteredBookings: List<Booking>
        get() = _state.value.filter?.let { f -> _state.value.bookings.filter { it.status == f } } ?: _state.value.bookings
}
