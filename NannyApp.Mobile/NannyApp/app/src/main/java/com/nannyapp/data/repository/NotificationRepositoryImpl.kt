package com.nannyapp.data.repository

import com.nannyapp.data.api.NotificationApi
import com.nannyapp.data.db.dao.NotificationDao
import com.nannyapp.domain.model.AppNotification
import com.nannyapp.domain.repository.NotificationRepository
import com.nannyapp.util.Resource
import com.nannyapp.util.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import com.nannyapp.data.preferences.SessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val sessionManager: SessionManager,
    private val api: NotificationApi,
    private val dao: NotificationDao,
) : NotificationRepository {

    override fun getNotifications(): Flow<Resource<List<AppNotification>>> = sessionManager.authTokenFlow.flatMapLatest { token ->
        val id = sessionManager.currentUserId()
        if (token == null || id == null) flowOf(Resource.Success(emptyList()))
        else dao.observeForAccount(id).map { Resource.Success(it.map { row -> row.asDomain() }) }
    }

    override fun unreadCount(): Flow<Int> = sessionManager.authTokenFlow.flatMapLatest { token ->
        val id = sessionManager.currentUserId()
        if (token == null || id == null) flowOf(0) else dao.observeUnreadCount(id)
    }

    override suspend fun markRead(notificationId: Int) {
        dao.markRead(notificationId)
        runCatching { api.markRead(mapOf("id" to notificationId)) }
    }

    override suspend fun markAllRead() {
        dao.markAllRead()
        runCatching { api.markAllRead() }
    }

    override suspend fun refresh(): Resource<Unit> {
        val token = sessionManager.currentToken()
        val accountId = sessionManager.withCurrentSession(token) { sessionManager.currentUserId() } ?: return Resource.Error("Please log in.", code = 401)
        val result = safeApiCall { api.getNotifications() }
        if (result is Resource.Success) sessionManager.withCurrentSession(token) { dao.upsertAll(result.data.map { it.toEntity().copy(accountId = accountId) }) }
        return result.map { }
    }
}

