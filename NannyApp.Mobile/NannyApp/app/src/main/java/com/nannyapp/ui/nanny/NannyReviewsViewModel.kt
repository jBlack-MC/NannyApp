package com.nannyapp.ui.nanny

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.domain.model.NannyProfile
import com.nannyapp.domain.model.Review
import com.nannyapp.domain.repository.NannyRepository
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NannyReviewsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val profile: NannyProfile? = null,
    val reviews: List<Review> = emptyList(),
) {
    val distribution: Map<Int, Int> get() = (1..5).associateWith { star -> reviews.count { it.rating == star } }
}

@HiltViewModel
class NannyReviewsViewModel @Inject constructor(private val repository: NannyRepository) : ViewModel() {
    private val _state = MutableStateFlow(NannyReviewsUiState())
    val state: StateFlow<NannyReviewsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val profile = repository.getNannyDetail(0)
            val reviews = repository.getNannyReviews(0)
            _state.value = _state.value.copy(
                loading = false,
                profile = (profile as? Resource.Success)?.data,
                reviews = (reviews as? Resource.Success)?.data.orEmpty(),
                error = (profile as? Resource.Error)?.message,
            )
        }
    }
}
