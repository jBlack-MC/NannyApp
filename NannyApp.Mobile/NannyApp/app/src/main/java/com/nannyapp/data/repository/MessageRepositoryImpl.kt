package com.nannyapp.data.repository

import com.nannyapp.data.api.MessageApi
import com.nannyapp.data.api.dto.SendMessageRequestDto
import com.nannyapp.data.db.dao.ChatMessageDao
import com.nannyapp.data.preferences.SessionManager
import com.nannyapp.domain.model.ChatMessage
import com.nannyapp.domain.model.Conversation
import com.nannyapp.domain.repository.MessageRepository
import com.nannyapp.util.Resource
import com.nannyapp.util.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uses efficient polling rather than WebSockets, per request #21 â€” the
 * existing PHP backend has no persistent-connection support, so the chat
 * screen calls pollNewMessages() on a short interval (see MessagesViewModel).
 */
@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val api: MessageApi,
    private val dao: ChatMessageDao,
    private val sessionManager: SessionManager,
) : MessageRepository {

    override fun getConversations(): Flow<Resource<List<Conversation>>> = flow {
        emit(Resource.Loading)
        emit(safeApiCall { api.getConversations() }.map { list -> list.map { it.asDomain() } })
    }

    override fun getMessages(withUserId: Int): Flow<Resource<List<ChatMessage>>> = flow {
        emit(Resource.Loading)
        val myId = sessionManager.currentUserId() ?: 0
        
        // Return cached messages first, then fetch new ones
        val cached = dao.observeThread(myId, withUserId).first()
        if (cached.isNotEmpty()) {
            emit(Resource.Success(cached.map { it.asDomain(myId) }))
        }

        when (val result = safeApiCall { api.getMessages(withUserId) }) {
            is Resource.Success -> {
                dao.upsertAll(result.data.map { it.toEntity() })
                emit(Resource.Success(result.data.map { it.asDomain(myId) }))
            }
            is Resource.Error -> {
                if (cached.isEmpty()) emit(result)
            }
            Resource.Loading -> {}
        }
    }

    override suspend fun sendMessage(toUserId: Int, content: String): Resource<ChatMessage> {
        val myId = sessionManager.currentUserId() ?: 0
        val result = safeApiCall { api.sendMessage(SendMessageRequestDto(toUserId, content)) }
        if (result is Resource.Success) {
            dao.upsert(result.data.toEntity())
        }
        return result.map { it.asDomain(myId) }
    }

    override suspend fun pollNewMessages(withUserId: Int, sinceId: Int): Resource<List<ChatMessage>> {
        val myId = sessionManager.currentUserId() ?: 0
        val result = safeApiCall { api.pollMessages(withUserId, sinceId) }
        if (result is Resource.Success) {
            dao.upsertAll(result.data.map { it.toEntity() })
        }
        return result.map { list -> list.map { it.asDomain(myId) } }
    }

    override suspend fun markConversationRead(withUserId: Int) {
        runCatching { api.markRead(mapOf("with" to withUserId)) }
    }
}

