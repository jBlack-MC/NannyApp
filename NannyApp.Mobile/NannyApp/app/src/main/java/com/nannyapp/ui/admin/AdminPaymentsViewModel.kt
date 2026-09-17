package com.nannyapp.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.domain.model.Payment
import com.nannyapp.domain.model.PaymentStatus
import com.nannyapp.domain.repository.AdminRepository
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminPaymentsUiState(val loading: Boolean = true, val error: String? = null, val payments: List<Payment> = emptyList(), val statusFilter: PaymentStatus? = null)

@HiltViewModel
class AdminPaymentsViewModel @Inject constructor(private val repository: AdminRepository) : ViewModel() {
    private val _state = MutableStateFlow(AdminPaymentsUiState())
    val state: StateFlow<AdminPaymentsUiState> = _state.asStateFlow()

    init { load() }

    fun setFilter(status: PaymentStatus?) { _state.value = _state.value.copy(statusFilter = status); load() }

    fun load() {
        viewModelScope.launch {
            repository.getAllPayments(_state.value.statusFilter).collect { result ->
                _state.value = when (result) {
                    Resource.Loading -> _state.value.copy(loading = true, error = null)
                    is Resource.Success -> _state.value.copy(loading = false, payments = result.data)
                    is Resource.Error -> _state.value.copy(loading = false, error = result.message)
                }
            }
        }
    }

    fun refund(paymentId: Int) { viewModelScope.launch { repository.refundPayment(paymentId); load() } }
}
