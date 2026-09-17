package com.nannyapp.ui.nanny

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.domain.model.EarningsSummary
import com.nannyapp.domain.repository.NannyRepository
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NannyEarningsUiState(val loading: Boolean = true, val error: String? = null, val summary: EarningsSummary? = null)

@HiltViewModel
class NannyEarningsViewModel @Inject constructor(private val repository: NannyRepository) : ViewModel() {
    private val _state = MutableStateFlow(NannyEarningsUiState())
    val state: StateFlow<NannyEarningsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            when (val result = repository.getEarnings()) {
                is Resource.Success -> _state.value = _state.value.copy(loading = false, summary = result.data)
                is Resource.Error -> _state.value = _state.value.copy(loading = false, error = result.message)
                Resource.Loading -> {}
            }
        }
    }
}
