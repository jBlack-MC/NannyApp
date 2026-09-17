package com.nannyapp.data.repository

import com.nannyapp.data.api.MessageApi
import com.nannyapp.data.api.dto.SendMessageRequestDto
import com.nannyapp.data.preferences.SessionManager
import com.nannyapp.domain.model.ChatMessage
import com.nannyapp.domain.model.Conversation
import com.nannyapp.domain.repository.MessageRepository
import com.nannyapp.util.Resource
import com.nannyapp.util.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uses efficient polling rather than WebSockets, per request #21 — the
 * existing PHP backend has no persistent-connection support, so the chat
 * screen calls pollNewMessages() on a short interval (see MessagesViewModel).
 */
@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val api: MessageApi,
    private val sessionManager: SessionManager,
) : MessageRepository {

    override fun getConversations(): Flow<Resource<List<Conversation>>> = flow {
        emit(Resource.Loading)
        emit(safeApiCall { api.getConversations() }.map { list -> list.map { it.toDomain() } })
    }

    override fun getMessages(withUserId: Int): Flow<Resource<List<ChatMessage>>> = flow {
        emit(Resource.Loading)
        val myId = sessionManager.currentUserId() ?: 0
        emit(safeApiCall { api.getMessages(withUserId) }.map { list -> list.map { it.toDomain(myId) } })
    }

    override suspend fun sendMessage(toUserId: Int, content: String): Resource<ChatMessage> {
        val myId = sessionManager.currentUserId() ?: 0
        return safeApiCall { api.sendMessage(SendMessageRequestDto(toUserId, content)) }.map { it.toDomain(myId) }
    }

    override suspend fun pollNewMessages(withUserId: Int, sinceId: Int): Resource<List<ChatMessage>> {
        val myId = sessionManager.currentUserId() ?: 0
        return safeApiCall { api.pollMessages(withUserId, sinceId) }.map { list -> list.map { it.toDomain(myId) } }
    }

    override suspend fun markConversationRead(withUserId: Int) {
        runCatching { api.markRead(mapOf("with" to withUserId)) }
    }
}
