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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val api: NotificationApi,
    private val dao: NotificationDao,
) : NotificationRepository {

    override fun getNotifications(): Flow<Resource<List<AppNotification>>> = flow {
        dao.observeAll().collect { list -> emit(Resource.Success(list.map { it.asDomain() })) }
    }

    override fun unreadCount(): Flow<Int> = dao.observeUnreadCount()

    override suspend fun markRead(notificationId: Int) {
        dao.markRead(notificationId)
        runCatching { api.markRead(mapOf("id" to notificationId)) }
    }

    override suspend fun markAllRead() {
        dao.markAllRead()
        runCatching { api.markAllRead() }
    }

    override suspend fun refresh(): Resource<Unit> {
        val result = safeApiCall { api.getNotifications() }
        if (result is Resource.Success) dao.upsertAll(result.data.map { it.toEntity() })
        return result.map { }
    }
}

