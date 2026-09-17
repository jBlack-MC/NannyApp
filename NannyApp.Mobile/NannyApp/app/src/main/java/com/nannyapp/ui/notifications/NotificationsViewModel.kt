package com.nannyapp.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.domain.model.AppNotification
import com.nannyapp.domain.repository.NotificationRepository
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(val loading: Boolean = true, val notifications: List<AppNotification> = emptyList())

@HiltViewModel
class NotificationsViewModel @Inject constructor(private val repository: NotificationRepository) : ViewModel() {
    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { repository.refresh() }
        viewModelScope.launch {
            repository.getNotifications().collect { result ->
                if (result is Resource.Success) _state.value = _state.value.copy(loading = false, notifications = result.data)
            }
        }
    }

    fun markRead(id: Int) { viewModelScope.launch { repository.markRead(id) } }
    fun markAllRead() { viewModelScope.launch { repository.markAllRead() } }
    fun refresh() { viewModelScope.launch { repository.refresh() } }
}
