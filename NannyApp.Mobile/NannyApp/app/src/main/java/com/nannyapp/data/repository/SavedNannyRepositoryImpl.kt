package com.nannyapp.data.repository

import com.nannyapp.data.api.NannyApi
import com.nannyapp.domain.model.SavedNanny
import com.nannyapp.domain.repository.SavedNannyRepository
import com.nannyapp.util.Resource
import com.nannyapp.util.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedNannyRepositoryImpl @Inject constructor(private val api: NannyApi) : SavedNannyRepository {

    override fun getSavedNannies(): Flow<Resource<List<SavedNanny>>> = flow {
        emit(Resource.Loading)
        emit(safeApiCall { api.getSaved() }.map { list -> list.map { it.toDomain() } })
    }

    override suspend fun toggleSave(nannyId: Int, save: Boolean): Resource<Boolean> {
        val result = safeApiCall { api.toggleSave(mapOf("nanny_id" to nannyId, "save" to if (save) 1 else 0)) }
        return result.map { it["saved"] ?: save }
    }
}
