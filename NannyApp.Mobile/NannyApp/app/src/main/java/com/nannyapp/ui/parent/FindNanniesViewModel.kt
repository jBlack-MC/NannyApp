package com.nannyapp.ui.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.domain.model.*
import com.nannyapp.domain.repository.NannyRepository
import com.nannyapp.domain.repository.SavedNannyRepository
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FindNanniesUiState(
    val filters: NannySearchFilters = NannySearchFilters(),
    val nannies: List<NannyProfile> = emptyList(),
    val savedIds: Set<Int> = emptySet(),
    val loading: Boolean = true,
    val error: String? = null,
    val showFilterSheet: Boolean = false,
)

@HiltViewModel
class FindNanniesViewModel @Inject constructor(
    private val nannyRepository: NannyRepository,
    private val savedNannyRepository: SavedNannyRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FindNanniesUiState())
    val state: StateFlow<FindNanniesUiState> = _state.asStateFlow()

    init {
        search()
        refreshSaved()
    }

    fun onQueryChange(q: String) {
        _state.value = _state.value.copy(filters = _state.value.filters.copy(query = q))
        search()
    }

    fun updateFilters(transform: (NannySearchFilters) -> NannySearchFilters) {
        _state.value = _state.value.copy(filters = transform(_state.value.filters))
    }

    fun applyFilters() {
        _state.value = _state.value.copy(showFilterSheet = false)
        search()
    }

    fun toggleFilterSheet(show: Boolean) { _state.value = _state.value.copy(showFilterSheet = show) }

    fun search() {
        viewModelScope.launch {
            nannyRepository.searchNannies(_state.value.filters).collect { result ->
                when (result) {
                    Resource.Loading -> _state.value = _state.value.copy(loading = true, error = null)
                    is Resource.Success -> _state.value = _state.value.copy(loading = false, nannies = result.data)
                    is Resource.Error -> _state.value = _state.value.copy(loading = false, error = result.message)
                }
            }
        }
    }

    private fun refreshSaved() {
        viewModelScope.launch {
            savedNannyRepository.getSavedNannies().collect { result ->
                if (result is Resource.Success) {
                    _state.value = _state.value.copy(savedIds = result.data.map { it.nannyId }.toSet())
                }
            }
        }
    }

    fun toggleSave(nannyId: Int) {
        viewModelScope.launch {
            val currentlySaved = _state.value.savedIds.contains(nannyId)
            val result = savedNannyRepository.toggleSave(nannyId, !currentlySaved)
            if (result is Resource.Success) {
                val newSet = _state.value.savedIds.toMutableSet()
                if (result.data) newSet.add(nannyId) else newSet.remove(nannyId)
                _state.value = _state.value.copy(savedIds = newSet)
            }
        }
    }
}
