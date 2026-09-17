package com.nannyapp.ui.booking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.domain.repository.BookingRepository
import com.nannyapp.domain.repository.ReviewRepository
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeaveReviewUiState(
    val nannyId: Int? = null,
    val nannyName: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val submitted: Boolean = false,
)

@HiltViewModel
class LeaveReviewViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val reviewRepository: ReviewRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val bookingId: Int = checkNotNull(savedStateHandle["bookingId"])
    private val _state = MutableStateFlow(LeaveReviewUiState())
    val state: StateFlow<LeaveReviewUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val booking = bookingRepository.getBookingDetail(bookingId)
            if (booking is Resource.Success) {
                _state.value = _state.value.copy(nannyId = booking.data.nannyId, nannyName = booking.data.nannyName ?: "")
            }
        }
    }

    fun setRating(r: Int) { _state.value = _state.value.copy(rating = r) }
    fun setComment(c: String) { _state.value = _state.value.copy(comment = c) }

    fun submit() {
        val nannyId = _state.value.nannyId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            when (val result = reviewRepository.submitReview(bookingId, nannyId, _state.value.rating, _state.value.comment)) {
                is Resource.Success -> _state.value = _state.value.copy(loading = false, submitted = true)
                is Resource.Error -> _state.value = _state.value.copy(loading = false, error = result.message)
                Resource.Loading -> {}
            }
        }
    }
}
