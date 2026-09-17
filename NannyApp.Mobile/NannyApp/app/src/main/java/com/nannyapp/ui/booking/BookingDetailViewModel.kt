package com.nannyapp.ui.booking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.data.preferences.SessionManager
import com.nannyapp.domain.model.Booking
import com.nannyapp.domain.model.UserRole
import com.nannyapp.domain.repository.BookingRepository
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookingDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val actionError: String? = null,
    val booking: Booking? = null,
    val role: UserRole = UserRole.PARENT,
    val actionInProgress: Boolean = false,
)

@HiltViewModel
class BookingDetailViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val bookingId: Int = checkNotNull(savedStateHandle["bookingId"])
    private val _state = MutableStateFlow(BookingDetailUiState())
    val state: StateFlow<BookingDetailUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val role = sessionManager.currentRole() ?: UserRole.PARENT
            when (val result = bookingRepository.getBookingDetail(bookingId)) {
                is Resource.Success -> _state.value = _state.value.copy(loading = false, booking = result.data, role = role)
                is Resource.Error -> _state.value = _state.value.copy(loading = false, error = result.message)
                Resource.Loading -> {}
            }
        }
    }

    private fun runAction(block: suspend () -> Resource<*>) {
        viewModelScope.launch {
            _state.value = _state.value.copy(actionInProgress = true, actionError = null)
            when (val result = block()) {
                is Resource.Success -> load()
                is Resource.Error -> _state.value = _state.value.copy(actionInProgress = false, actionError = result.message)
                Resource.Loading -> {}
            }
        }
    }

    fun cancel() = runAction { bookingRepository.cancelBooking(bookingId) }
    fun accept() = runAction { bookingRepository.acceptBooking(bookingId) }
    fun reject() = runAction { bookingRepository.rejectBooking(bookingId) }
    fun checkOut() = runAction { bookingRepository.checkOut(bookingId) }
    fun confirmCompletion() = runAction { bookingRepository.confirmCompletion(bookingId) }
    fun dispute(reason: String) = runAction { bookingRepository.disputeBooking(bookingId, reason) }
}
