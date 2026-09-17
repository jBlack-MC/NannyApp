package com.nannyapp.ui.nanny

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.domain.model.AvailabilitySlot
import com.nannyapp.domain.model.DayAvailability
import com.nannyapp.domain.repository.NannyRepository
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

val DAY_NAMES = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

data class NannyAvailabilityUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val days: List<DayAvailability> = (0..6).map { DayAvailability(it, false, "09:00", "17:00") },
)

@HiltViewModel
class NannyAvailabilityViewModel @Inject constructor(private val repository: NannyRepository) : ViewModel() {
    private val _state = MutableStateFlow(NannyAvailabilityUiState())
    val state: StateFlow<NannyAvailabilityUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            when (val result = repository.getAvailability(0)) {
                is Resource.Success -> {
                    val byDay = result.data.associateBy { it.dayOfWeek }
                    val merged = (0..6).map { d -> byDay[d] ?: DayAvailability(d, false, "09:00", "17:00") }
                    _state.value = _state.value.copy(loading = false, days = merged)
                }
                is Resource.Error -> _state.value = _state.value.copy(loading = false, error = result.message)
                Resource.Loading -> {}
            }
        }
    }

    fun toggleDay(day: Int, available: Boolean) {
        _state.value = _state.value.copy(days = _state.value.days.map { if (it.dayOfWeek == day) it.copy(isAvailable = available) else it })
    }

    fun setTimeRange(day: Int, start: String, end: String) {
        _state.value = _state.value.copy(days = _state.value.days.map { if (it.dayOfWeek == day) it.copy(timeStart = start, timeEnd = end) else it })
    }

    fun toggleSlot(day: Int, slot: AvailabilitySlot) {
        _state.value = _state.value.copy(days = _state.value.days.map {
            if (it.dayOfWeek == day) {
                val newSlots = if (it.slots.contains(slot)) it.slots - slot else it.slots + slot
                it.copy(slots = newSlots)
            } else it
        })
    }

    fun save() {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null, saved = false)
            when (val result = repository.setAvailability(_state.value.days)) {
                is Resource.Success -> _state.value = _state.value.copy(saving = false, saved = true)
                is Resource.Error -> _state.value = _state.value.copy(saving = false, error = result.message)
                Resource.Loading -> {}
            }
        }
    }
}
