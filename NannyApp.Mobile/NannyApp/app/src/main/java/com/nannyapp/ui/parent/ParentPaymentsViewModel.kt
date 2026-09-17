package com.nannyapp.ui.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.domain.model.Payment
import com.nannyapp.domain.repository.PaymentRepository
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ParentPaymentsUiState(val loading: Boolean = true, val error: String? = null, val payments: List<Payment> = emptyList())

@HiltViewModel
class ParentPaymentsViewModel @Inject constructor(private val repository: PaymentRepository) : ViewModel() {
    private val _state = MutableStateFlow(ParentPaymentsUiState())
    val state: StateFlow<ParentPaymentsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getPaymentsForRole().collect { result ->
                _state.value = when (result) {
                    Resource.Loading -> _state.value.copy(loading = true, error = null)
                    is Resource.Success -> _state.value.copy(loading = false, payments = result.data)
                    is Resource.Error -> _state.value.copy(loading = false, error = result.message)
                }
            }
        }
    }
}
