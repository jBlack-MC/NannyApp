package com.nannyapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.domain.model.HomeContent
import com.nannyapp.domain.repository.StaticContentRepository
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Loads the website's home-page photos, nannies and testimonials. Offline, the screen keeps its bundled photos. */
@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val content: StaticContentRepository,
) : ViewModel() {
    private val _home = MutableStateFlow(HomeContent())
    val home: StateFlow<HomeContent> = _home.asStateFlow()

    init {
        viewModelScope.launch {
            (content.getHomeContent() as? Resource.Success)?.let { _home.value = it.data }
        }
    }
}
