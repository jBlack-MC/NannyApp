package com.nannyapp.ui.parent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.domain.model.NannyProfile
import com.nannyapp.domain.model.Review
import com.nannyapp.domain.repository.NannyRepository
import com.nannyapp.domain.repository.SavedNannyRepository
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NannyDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val nanny: NannyProfile? = null,
    val reviews: List<Review> = emptyList(),
    val isSaved: Boolean = false,
)

@HiltViewModel
class NannyDetailViewModel @Inject constructor(
    private val nannyRepository: NannyRepository,
    private val savedNannyRepository: SavedNannyRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val nannyId: Int = checkNotNull(savedStateHandle["nannyId"])
    private val _state = MutableStateFlow(NannyDetailUiState())
    val state: StateFlow<NannyDetailUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val detail = nannyRepository.getNannyDetail(nannyId)
            if (detail is Resource.Error) {
                _state.value = _state.value.copy(loading = false, error = detail.message)
                return@launch
            }
            val reviews = nannyRepository.getNannyReviews(nannyId)
            val savedList = savedNannyRepository.getSavedNannies().first { it !is Resource.Loading }
            val isSaved = (savedList as? Resource.Success)?.data?.any { it.nannyId == nannyId } ?: false

            _state.value = _state.value.copy(
                loading = false,
                nanny = (detail as Resource.Success).data,
                reviews = (reviews as? Resource.Success)?.data.orEmpty(),
                isSaved = isSaved,
            )
        }
    }

    fun toggleSave() {
        viewModelScope.launch {
            val result = savedNannyRepository.toggleSave(nannyId, !_state.value.isSaved)
            if (result is Resource.Success) _state.value = _state.value.copy(isSaved = result.data)
        }
    }
}
