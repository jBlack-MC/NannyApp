package com.nannyapp.ui.parent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannyapp.domain.model.SavedNanny
import com.nannyapp.domain.repository.SavedNannyRepository
import com.nannyapp.ui.components.*
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

data class SavedNanniesUiState(val loading: Boolean = true, val error: String? = null, val saved: List<SavedNanny> = emptyList())

@HiltViewModel
class SavedNanniesViewModel @Inject constructor(private val repository: SavedNannyRepository) : ViewModel() {
    private val _state = MutableStateFlow(SavedNanniesUiState())
    val state: StateFlow<SavedNanniesUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            repository.getSavedNannies().collect { result ->
                _state.value = when (result) {
                    Resource.Loading -> _state.value.copy(loading = true, error = null)
                    is Resource.Success -> _state.value.copy(loading = false, saved = result.data)
                    is Resource.Error -> _state.value.copy(loading = false, error = result.message)
                }
            }
        }
    }

    fun unsave(nannyId: Int) {
        viewModelScope.launch { repository.toggleSave(nannyId, false); load() }
    }
}

@Composable
fun SavedNanniesScreen(onBack: () -> Unit, onNannyClick: (Int) -> Unit, viewModel: SavedNanniesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { AppTopBar(title = "Saved Nannies", onBack = onBack) }) { padding ->
        when {
            state.loading -> LoadingView(modifier = Modifier.padding(padding))
            state.error != null -> ErrorState(message = state.error!!, modifier = Modifier.padding(padding), onRetry = viewModel::load)
            state.saved.isEmpty() -> EmptyState(title = "No saved nannies", message = "Tap the bookmark icon on a nanny's profile to save them here.", icon = Icons.Filled.BookmarkBorder, modifier = Modifier.padding(padding))
            else -> LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.saved, key = { it.id }) { saved ->
                    saved.nanny?.let { nanny ->
                        NannyCard(nanny = nanny, isSaved = true, onClick = { onNannyClick(nanny.userId) }, onToggleSave = { viewModel.unsave(nanny.userId) })
                    }
                }
            }
        }
    }
}
