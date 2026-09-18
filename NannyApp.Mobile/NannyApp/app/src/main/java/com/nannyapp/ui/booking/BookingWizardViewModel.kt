package com.nannyapp.ui.booking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.domain.model.*
import com.nannyapp.domain.repository.BookingRepository
import com.nannyapp.domain.repository.ChildRepository
import com.nannyapp.domain.repository.NannyRepository
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the 8-step wizard described in request #12, mirroring the session-backed
 * flow in parent/book.php: nanny -> date -> time/duration -> children -> address ->
 * notes -> summary -> payment. Step 1 (nanny) arrives pre-selected via the route arg.
 */
data class BookingWizardUiState(
    val step: Int = 1,
    val totalSteps: Int = 8,
    val wizard: BookingWizardState = BookingWizardState(),
    val children: List<Child> = emptyList(),
    val loadingNanny: Boolean = true,
    val submitting: Boolean = false,
    val error: String? = null,
    val createdBooking: Booking? = null,
)

@HiltViewModel
class BookingWizardViewModel @Inject constructor(
    private val nannyRepository: NannyRepository,
    private val childRepository: ChildRepository,
    private val bookingRepository: BookingRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val nannyId: Int = checkNotNull(savedStateHandle["nannyId"])
    private val _state = MutableStateFlow(BookingWizardUiState())
    val state: StateFlow<BookingWizardUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val nannyResult = nannyRepository.getNannyDetail(nannyId)
            val childrenResult = childRepository.getChildren().first { it !is Resource.Loading }
            _state.value = _state.value.copy(
                loadingNanny = false,
                wizard = _state.value.wizard.copy(nanny = (nannyResult as? Resource.Success)?.data),
                children = (childrenResult as? Resource.Success)?.data.orEmpty(),
                error = (nannyResult as? Resource.Error)?.message,
            )
        }
    }

    fun nextStep() { if (_state.value.step < _state.value.totalSteps) _state.value = _state.value.copy(step = _state.value.step + 1, error = null) }
    fun previousStep() { if (_state.value.step > 1) _state.value = _state.value.copy(step = _state.value.step - 1, error = null) }

    fun setDateTime(iso: String) { _state.value = _state.value.copy(wizard = _state.value.wizard.copy(dateTimeIso = iso)) }
    fun setDuration(hours: Double) { _state.value = _state.value.copy(wizard = _state.value.wizard.copy(durationHours = hours)) }
    fun toggleChild(id: Int) {
        val current = _state.value.wizard.selectedChildIds
        val updated = if (current.contains(id)) current - id else current + id
        _state.value = _state.value.copy(wizard = _state.value.wizard.copy(selectedChildIds = updated))
    }
    fun setAddress(v: String) { _state.value = _state.value.copy(wizard = _state.value.wizard.copy(address = v)) }
    fun setNotes(v: String) { _state.value = _state.value.copy(wizard = _state.value.wizard.copy(notes = v)) }

    fun validateStep(): Boolean {
        val w = _state.value.wizard
        val error = when (_state.value.step) {
            2 -> if (w.dateTimeIso.isNullOrBlank()) "Please select a date and time." else null
            4 -> if (w.selectedChildIds.isEmpty()) "Please select at least one child." else null
            5 -> if (w.address.isBlank()) "Please enter the booking address." else null
            else -> null
        }
        _state.value = _state.value.copy(error = error)
        return error == null
    }

    /** Creates a pending booking. Payment is arranged manually for the launch phase. */
    fun confirmAndPay() {
        viewModelScope.launch {
            _state.value = _state.value.copy(submitting = true, error = null)
            when (val result = bookingRepository.createBooking(_state.value.wizard)) {
                is Resource.Success -> _state.value = _state.value.copy(submitting = false, createdBooking = result.data)
                is Resource.Error -> _state.value = _state.value.copy(submitting = false, error = result.message)
                Resource.Loading -> {}
            }
        }
    }
}
