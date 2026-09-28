package com.nannyapp.data.repository

import com.nannyapp.data.api.NannyApi
import com.nannyapp.data.db.dao.SavedNannyDao
import com.nannyapp.domain.model.SavedNanny
import com.nannyapp.domain.repository.SavedNannyRepository
import com.nannyapp.util.Resource
import com.nannyapp.util.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import com.nannyapp.data.preferences.SessionManager
import com.nannyapp.util.canUseOfflineCache
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedNannyRepositoryImpl @Inject constructor(
    private val sessionManager: SessionManager,
    private val api: NannyApi,
    private val dao: SavedNannyDao,
) : SavedNannyRepository {

    override fun getSavedNannies(): Flow<Resource<List<SavedNanny>>> = flow {
        emit(Resource.Loading)
        val token = sessionManager.currentToken()
        val accountId = sessionManager.withCurrentSession(token) { sessionManager.currentUserId() } ?: return@flow emit(Resource.Error("Please log in.", code = 401))
        val result = safeApiCall { api.getSaved() }
        val output = sessionManager.withCurrentSession(token) {
            when (result) {
                is Resource.Success -> {
                    dao.upsertAll(result.data.map { it.toEntity().copy(accountId = accountId) })
                    Resource.Success(result.data.map { it.asDomain() })
                }
                is Resource.Error -> {
                    if (result.canUseOfflineCache()) {
                        val cached = dao.observeForAccount(accountId).first()
                        if (cached.isNotEmpty()) Resource.Success(cached.map { it.asDomain() }) else result
                    } else result
                }
                Resource.Loading -> Resource.Loading
            }
        }
        emit(output ?: Resource.Error("Your session has changed. Please log in again.", code = 401))
    }

    override suspend fun toggleSave(nannyId: Int, save: Boolean): Resource<Boolean> {
        val result = safeApiCall { api.toggleSave(mapOf("nanny_id" to nannyId, "save" to if (save) 1 else 0)) }
        return result.map { it["saved"] ?: save }
    }
}

