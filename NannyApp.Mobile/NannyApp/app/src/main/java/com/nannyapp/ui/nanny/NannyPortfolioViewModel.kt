package com.nannyapp.ui.nanny

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.domain.model.PortfolioItem
import com.nannyapp.domain.model.PortfolioType
import com.nannyapp.domain.repository.NannyRepository
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NannyPortfolioUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val items: List<PortfolioItem> = emptyList(),
    val uploading: Boolean = false,
)

@HiltViewModel
class NannyPortfolioViewModel @Inject constructor(private val repository: NannyRepository) : ViewModel() {
    private val _state = MutableStateFlow(NannyPortfolioUiState())
    val state: StateFlow<NannyPortfolioUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            when (val result = repository.getPortfolio(0)) {
                is Resource.Success -> _state.value = _state.value.copy(loading = false, items = result.data)
                is Resource.Error -> _state.value = _state.value.copy(loading = false, error = result.message)
                Resource.Loading -> {}
            }
        }
    }

    fun upload(type: PortfolioType, title: String, bytes: ByteArray, fileName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(uploading = true)
            val result = repository.uploadPortfolioItem(type, title, bytes, fileName)
            _state.value = _state.value.copy(uploading = false)
            if (result is Resource.Success) load()
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch { repository.deletePortfolioItem(id); load() }
    }
}
