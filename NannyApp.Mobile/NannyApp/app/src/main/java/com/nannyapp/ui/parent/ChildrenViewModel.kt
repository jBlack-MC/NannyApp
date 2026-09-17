package com.nannyapp.ui.parent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.domain.model.Child
import com.nannyapp.domain.repository.ChildRepository
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChildrenUiState(val loading: Boolean = true, val error: String? = null, val children: List<Child> = emptyList())

@HiltViewModel
class ChildrenViewModel @Inject constructor(private val repository: ChildRepository) : ViewModel() {
    private val _state = MutableStateFlow(ChildrenUiState())
    val state: StateFlow<ChildrenUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            repository.getChildren().collect { result ->
                _state.value = when (result) {
                    Resource.Loading -> _state.value.copy(loading = true, error = null)
                    is Resource.Success -> _state.value.copy(loading = false, children = result.data)
                    is Resource.Error -> _state.value.copy(loading = false, error = result.message)
                }
            }
        }
    }

    fun deleteChild(id: Int) {
        viewModelScope.launch { repository.deleteChild(id); load() }
    }
}

data class ChildFormUiState(
    val id: Int? = null,
    val name: String = "",
    val age: String = "",
    val gender: String = "",
    val allergies: String = "",
    val medicalConditions: String = "",
    val specialNeeds: String = "",
    val favouriteActivities: String = "",
    val notesForNannies: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class ChildFormViewModel @Inject constructor(
    private val repository: ChildRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val incomingId: Int? = savedStateHandle.get<Int>("childId")?.takeIf { it >= 0 }

    private val _state = MutableStateFlow(ChildFormUiState(id = incomingId))
    val state: StateFlow<ChildFormUiState> = _state.asStateFlow()

    init {
        if (incomingId != null) {
            viewModelScope.launch {
                repository.getChildren().collect { result ->
                    if (result is Resource.Success) {
                        result.data.find { it.id == incomingId }?.let { c ->
                            _state.value = _state.value.copy(
                                name = c.name, age = c.age?.toString() ?: "", gender = c.gender ?: "",
                                allergies = c.allergies ?: "", medicalConditions = c.medicalConditions ?: "",
                                specialNeeds = c.specialNeeds ?: "", favouriteActivities = c.favouriteActivities ?: "",
                                notesForNannies = c.notesForNannies ?: "",
                            )
                        }
                    }
                }
            }
        }
    }

    fun update(transform: (ChildFormUiState) -> ChildFormUiState) {
        _state.value = transform(_state.value).copy(error = null)
    }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) { _state.value = s.copy(error = "Please enter the child's name."); return }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val child = Child(
                id = s.id ?: 0, parentId = 0, name = s.name.trim(), age = s.age.toIntOrNull(), gender = s.gender.ifBlank { null },
                allergies = s.allergies.ifBlank { null }, medicalConditions = s.medicalConditions.ifBlank { null },
                specialNeeds = s.specialNeeds.ifBlank { null }, favouriteActivities = s.favouriteActivities.ifBlank { null },
                notesForNannies = s.notesForNannies.ifBlank { null },
            )
            val result = if (s.id == null) repository.addChild(child) else repository.updateChild(child)
            _state.value = when (result) {
                is Resource.Success -> _state.value.copy(loading = false, saved = true)
                is Resource.Error -> _state.value.copy(loading = false, error = result.message)
                Resource.Loading -> _state.value
            }
        }
    }
}
