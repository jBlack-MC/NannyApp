package com.nannyapp.data.repository

import com.nannyapp.data.api.ChildApi
import com.nannyapp.data.db.dao.ChildDao
import com.nannyapp.domain.model.Child
import com.nannyapp.domain.repository.ChildRepository
import com.nannyapp.util.Resource
import com.nannyapp.util.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import com.nannyapp.data.preferences.SessionManager
import com.nannyapp.util.canUseOfflineCache
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChildRepositoryImpl @Inject constructor(
    private val sessionManager: SessionManager,
    private val api: ChildApi,
    private val dao: ChildDao,
) : ChildRepository {

    override fun getChildren(): Flow<Resource<List<Child>>> = flow {
        emit(Resource.Loading)
        val token = sessionManager.currentToken()
        val accountId = sessionManager.withCurrentSession(token) { sessionManager.currentUserId() } ?: return@flow emit(Resource.Error("Please log in.", code = 401))
        val result = safeApiCall { api.getChildren() }
        val output = sessionManager.withCurrentSession(token) {
            when (result) {
                is Resource.Success -> {
                    dao.upsertAll(result.data.map { it.toEntity() })
                    Resource.Success(result.data.map { it.asDomain() })
                }
                is Resource.Error -> {
                    if (result.canUseOfflineCache()) {
                        val cached = dao.observeForParent(accountId).first()
                        if (cached.isNotEmpty()) Resource.Success(cached.map { it.asDomain() }) else result
                    } else result
                }
                Resource.Loading -> Resource.Loading
            }
        }
        emit(output ?: Resource.Error("Your session has changed. Please log in again.", code = 401))
    }

    override suspend fun addChild(child: Child): Resource<Child> {
        val requestToken = sessionManager.currentToken()
        val result = safeApiCall { api.addChild(child.toDto()) }
        if (result is Resource.Success) sessionManager.withCurrentSession(requestToken) { dao.upsert(result.data.toEntity()) }
        return result.map { it.asDomain() }
    }

    override suspend fun updateChild(child: Child): Resource<Child> {
        val requestToken = sessionManager.currentToken()
        val result = safeApiCall { api.updateChild(child.toDto()) }
        if (result is Resource.Success) sessionManager.withCurrentSession(requestToken) { dao.upsert(result.data.toEntity()) }
        return result.map { it.asDomain() }
    }

    override suspend fun deleteChild(childId: Int): Resource<Unit> {
        val result = safeApiCall { api.deleteChild(childId) }
        if (result is Resource.Success) dao.delete(childId)
        return result
    }
}

