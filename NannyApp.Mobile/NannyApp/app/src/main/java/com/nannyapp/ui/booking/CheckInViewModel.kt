package com.nannyapp.ui.booking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.domain.model.Booking
import com.nannyapp.domain.repository.BookingRepository
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Mirrors the PIN handshake in nanny/bookings.php: up to 5 attempts before the
 * server locks the booking and flags it for admin review.
 */
data class CheckInUiState(
    val pin: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val attemptsRemaining: Int = 5,
    val success: Booking? = null,
)

@HiltViewModel
class CheckInViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val bookingId: Int = checkNotNull(savedStateHandle["bookingId"])
    private val _state = MutableStateFlow(CheckInUiState())
    val state: StateFlow<CheckInUiState> = _state.asStateFlow()

    fun onPinChange(v: String) {
        if (v.length <= 6) _state.value = _state.value.copy(pin = v.uppercase(), error = null)
    }

    fun submit() {
        val pin = _state.value.pin
        if (pin.length < 4) {
            _state.value = _state.value.copy(error = "Enter the PIN the parent gave you.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            when (val result = bookingRepository.checkIn(bookingId, pin)) {
                is Resource.Success -> _state.value = _state.value.copy(loading = false, success = result.data)
                is Resource.Error -> _state.value = _state.value.copy(
                    loading = false,
                    error = result.message,
                    attemptsRemaining = (_state.value.attemptsRemaining - 1).coerceAtLeast(0),
                )
                Resource.Loading -> {}
            }
        }
    }
}
