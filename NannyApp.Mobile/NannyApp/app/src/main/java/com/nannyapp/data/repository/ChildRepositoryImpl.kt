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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChildRepositoryImpl @Inject constructor(
    private val api: ChildApi,
    private val dao: ChildDao,
) : ChildRepository {

    override fun getChildren(): Flow<Resource<List<Child>>> = flow {
        emit(Resource.Loading)
        when (val result = safeApiCall { api.getChildren() }) {
            is Resource.Success -> {
                dao.upsertAll(result.data.map { it.toEntity() })
                emit(Resource.Success(result.data.map { it.toDomain() }))
            }
            is Resource.Error -> {
                // fall back to cache when offline, per request #33 offline states
                val cached = dao.observeAllCached().first()
                if (cached.isNotEmpty()) emit(Resource.Success(cached.map { it.toDomain() })) else emit(result)
            }
            Resource.Loading -> {}
        }
    }

    override suspend fun addChild(child: Child): Resource<Child> {
        val result = safeApiCall { api.addChild(child.toDto()) }
        if (result is Resource.Success) dao.upsert(result.data.toEntity())
        return result.map { it.toDomain() }
    }

    override suspend fun updateChild(child: Child): Resource<Child> {
        val result = safeApiCall { api.updateChild(child.toDto()) }
        if (result is Resource.Success) dao.upsert(result.data.toEntity())
        return result.map { it.toDomain() }
    }

    override suspend fun deleteChild(childId: Int): Resource<Unit> {
        val result = safeApiCall { api.deleteChild(childId) }
        if (result is Resource.Success) dao.delete(childId)
        return result
    }
}
