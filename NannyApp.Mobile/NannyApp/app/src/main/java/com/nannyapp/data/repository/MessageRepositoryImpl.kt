package com.nannyapp.data.repository

import com.nannyapp.data.api.MessageApi
import com.nannyapp.data.api.dto.SendMessageRequestDto
import com.nannyapp.data.db.dao.ChatMessageDao
import com.nannyapp.data.preferences.SessionManager
import com.nannyapp.domain.model.ChatMessage
import com.nannyapp.domain.model.Conversation
import com.nannyapp.domain.repository.MessageRepository
import com.nannyapp.util.canUseOfflineCache
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
        val token = sessionManager.currentToken()
        val myId = sessionManager.withCurrentSession(token) { sessionManager.currentUserId() }
            ?: return@flow emit(Resource.Error("Please log in.", code = 401))
        val result = safeApiCall { api.getMessages(withUserId) }
        val output = sessionManager.withCurrentSession(token) {
            when (result) {
                is Resource.Success -> {
                    dao.upsertAll(result.data.map { it.toEntity() })
                    Resource.Success(result.data.map { it.asDomain(myId) })
                }
                is Resource.Error -> {
                    if (result.canUseOfflineCache()) {
                        val cached = dao.observeThread(myId, withUserId).first()
                        if (cached.isNotEmpty()) Resource.Success(cached.map { it.asDomain(myId) }) else result
                    } else result
                }
                Resource.Loading -> Resource.Loading
            }
        }
        emit(output ?: Resource.Error("Your session has changed.", code = 401))
    }

    override suspend fun sendMessage(toUserId: Int, content: String): Resource<ChatMessage> {
        val token = sessionManager.currentToken()
        val myId = sessionManager.withCurrentSession(token) { sessionManager.currentUserId() } ?: return Resource.Error("Please log in.", code = 401)
        val result = safeApiCall { api.sendMessage(SendMessageRequestDto(toUserId, content)) }
        if (result is Resource.Success) {
            sessionManager.withCurrentSession(token) { dao.upsert(result.data.toEntity()) }
        }
        return result.map { it.asDomain(myId) }
    }

    override suspend fun pollNewMessages(withUserId: Int, sinceId: Int): Resource<List<ChatMessage>> {
        val token = sessionManager.currentToken()
        val myId = sessionManager.withCurrentSession(token) { sessionManager.currentUserId() } ?: return Resource.Error("Please log in.", code = 401)
        val result = safeApiCall { api.pollMessages(withUserId, sinceId) }
        if (result is Resource.Success) {
            sessionManager.withCurrentSession(token) { dao.upsertAll(result.data.map { it.toEntity() }) }
        }
        return result.map { list -> list.map { it.asDomain(myId) } }
    }

    override suspend fun markConversationRead(withUserId: Int) {
        runCatching { api.markRead(mapOf("with" to withUserId)) }
    }
}

