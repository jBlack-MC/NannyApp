package com.nannyapp.ui.misc

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.domain.repository.StaticContentRepository
import com.nannyapp.ui.components.AppTopBar
import com.nannyapp.ui.components.ErrorState
import com.nannyapp.ui.components.LoadingView
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Renders the CMS-backed informational pages (About/Services/Safety/FAQ/
 * Contact/Community/Resources/Pricing/Terms/Privacy) which map 1:1 to rows
 * in the existing `page_content` table.
 */
@HiltViewModel
class StaticPageViewModel @Inject constructor(
    private val repository: StaticContentRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<Resource<Pair<String, String>>>(Resource.Loading)
    val state: StateFlow<Resource<Pair<String, String>>> = _state.asStateFlow()

    fun load(pageKey: String) {
        viewModelScope.launch {
            _state.value = Resource.Loading
            _state.value = repository.getPage(pageKey)
        }
    }
}

@Composable
fun StaticPageScreen(pageKey: String, onBack: () -> Unit, viewModel: StaticPageViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(pageKey) { viewModel.load(pageKey) }

    val title = pageKey.replaceFirstChar { it.uppercase() }
    Scaffold(topBar = { AppTopBar(title = title, onBack = onBack) }) { padding ->
        Box(Modifier.padding(padding)) {
            when (val s = state) {
                Resource.Loading -> LoadingView()
                is Resource.Error -> ErrorState(message = s.message, onRetry = { viewModel.load(pageKey) })
                is Resource.Success -> Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
                    Text(s.data.first, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(12.dp))
                    Text(s.data.second, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
