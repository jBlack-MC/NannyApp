package com.nannyapp.ui.messaging

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.domain.model.ChatMessage
import com.nannyapp.domain.repository.MessageRepository
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val sending: Boolean = false,
    val peerName: String = "Chat",
)

/** Polls every 4s while the screen is open — the pragmatic choice for the
 * existing PHP backend (request #21: "efficient polling" when WebSockets
 * aren't practical). */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: MessageRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val withUserId: Int = checkNotNull(savedStateHandle["userId"])
    private val suppliedPeerName: String = savedStateHandle.get<String>("peerName") ?: ""
    private val _state = MutableStateFlow(ChatUiState(peerName = suppliedPeerName.ifBlank { "Chat" }))
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    init {
        loadInitial()
        startPolling()
        if (suppliedPeerName.isBlank()) resolvePeerNameFromConversations()
    }

    /** Falls back to the conversations list to resolve a display name when the
     * caller didn't already know it (e.g. deep link, or a brand-new thread). */
    private fun resolvePeerNameFromConversations() {
        viewModelScope.launch {
            val result = repository.getConversations().let { flow ->
                var last: Resource<List<com.nannyapp.domain.model.Conversation>>? = null
                flow.collect { r -> if (r !is Resource.Loading) last = r }
                last
            }
            val match = (result as? Resource.Success)?.data?.firstOrNull { it.withUserId == withUserId }
            if (match != null) {
                _state.value = _state.value.copy(peerName = match.withUserName)
            }
        }
    }

    private fun loadInitial() {
        viewModelScope.launch {
            repository.getMessages(withUserId).collect { result ->
                _state.value = when (result) {
                    Resource.Loading -> _state.value.copy(loading = true, error = null)
                    is Resource.Success -> _state.value.copy(loading = false, messages = result.data)
                    is Resource.Error -> _state.value.copy(loading = false, error = result.message)
                }
            }
            repository.markConversationRead(withUserId)
        }
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (isActive) {
                delay(4000)
                val lastId = _state.value.messages.lastOrNull()?.id ?: 0
                val result = repository.pollNewMessages(withUserId, lastId)
                if (result is Resource.Success && result.data.isNotEmpty()) {
                    _state.value = _state.value.copy(messages = _state.value.messages + result.data)
                    repository.markConversationRead(withUserId)
                }
            }
        }
    }

    fun onDraftChange(v: String) { _state.value = _state.value.copy(draft = v) }

    fun send() {
        val content = _state.value.draft.trim()
        if (content.isEmpty()) return
        _state.value = _state.value.copy(draft = "", sending = true)
        viewModelScope.launch {
            when (val result = repository.sendMessage(withUserId, content)) {
                is Resource.Success -> _state.value = _state.value.copy(sending = false, messages = _state.value.messages + result.data)
                is Resource.Error -> _state.value = _state.value.copy(sending = false, error = result.message)
                Resource.Loading -> {}
            }
        }
    }
}
