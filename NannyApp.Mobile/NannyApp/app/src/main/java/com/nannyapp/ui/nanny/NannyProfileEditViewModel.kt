package com.nannyapp.ui.nanny

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.domain.model.NannyProfile
import com.nannyapp.domain.repository.NannyRepository
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NannyProfileEditUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val bio: String = "",
    val experienceYears: String = "",
    val hourlyRate: String = "",
    val location: String = "",
    val skills: String = "",
    val languages: String = "",
    val qualifications: String = "",
    val specialisations: String = "",
    val availabilityText: String = "",
)

@HiltViewModel
class NannyProfileEditViewModel @Inject constructor(private val repository: NannyRepository) : ViewModel() {
    private val _state = MutableStateFlow(NannyProfileEditUiState())
    val state: StateFlow<NannyProfileEditUiState> = _state.asStateFlow()
    private var loaded: NannyProfile? = null

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            when (val result = repository.getNannyDetail(0)) {
                is Resource.Success -> {
                    val p = result.data
                    loaded = p
                    _state.value = _state.value.copy(
                        loading = false, bio = p.bio ?: "", experienceYears = p.experienceYears.toString(),
                        hourlyRate = p.hourlyRate.toString(), location = p.location ?: "",
                        skills = p.skills.joinToString(","), languages = p.languages.joinToString(","),
                        qualifications = p.qualifications ?: "", specialisations = p.specialisations.joinToString(","),
                        availabilityText = p.availabilityText ?: "",
                    )
                }
                is Resource.Error -> _state.value = _state.value.copy(loading = false, error = result.message)
                Resource.Loading -> {}
            }
        }
    }

    fun update(transform: (NannyProfileEditUiState) -> NannyProfileEditUiState) { _state.value = transform(_state.value).copy(error = null) }

    fun save() {
        val s = _state.value
        val base = loaded ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null)
            val updated = base.copy(
                bio = s.bio, experienceYears = s.experienceYears.toIntOrNull() ?: base.experienceYears,
                hourlyRate = s.hourlyRate.toDoubleOrNull() ?: base.hourlyRate, location = s.location,
                skills = s.skills.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                languages = s.languages.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                qualifications = s.qualifications,
                specialisations = s.specialisations.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                availabilityText = s.availabilityText,
            )
            when (val result = repository.updateOwnProfile(updated)) {
                is Resource.Success -> _state.value = _state.value.copy(saving = false, saved = true)
                is Resource.Error -> _state.value = _state.value.copy(saving = false, error = result.message)
                Resource.Loading -> {}
            }
        }
    }
}
