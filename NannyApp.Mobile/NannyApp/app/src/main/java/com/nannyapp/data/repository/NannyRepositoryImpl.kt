package com.nannyapp.data.repository

import com.nannyapp.data.api.NannyApi
import com.nannyapp.data.db.dao.NannyProfileDao
import com.nannyapp.domain.model.*
import com.nannyapp.domain.repository.NannyRepository
import com.nannyapp.util.Resource
import com.nannyapp.util.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NannyRepositoryImpl @Inject constructor(
    private val api: NannyApi,
    private val nannyProfileDao: NannyProfileDao,
) : NannyRepository {

    override fun searchNannies(filters: NannySearchFilters): Flow<Resource<List<NannyProfile>>> = flow {
        emit(Resource.Loading)
        val sort = when (filters.sortBy) {
            NannySortOption.RATING -> "rating"
            NannySortOption.EXPERIENCE -> "experience"
            NannySortOption.PRICE_LOW -> "price_low"
            NannySortOption.PRICE_HIGH -> "price_high"
        }
        val result = safeApiCall {
            api.searchNannies(
                query = filters.query.ifBlank { null },
                location = filters.location,
                minRate = filters.minRate,
                maxRate = filters.maxRate,
                minExperience = filters.minExperience,
                minRating = filters.minRating,
                verifiedOnly = if (filters.verifiedOnly) true else null,
                slot = filters.availableSlot?.toApi(),
                sort = sort,
            )
        }
        when (result) {
            is Resource.Success -> {
                nannyProfileDao.upsertAll(result.data.map { it.toEntity() })
                emit(Resource.Success(result.data.map { it.toDomain() }))
            }
            is Resource.Error -> emit(result)
            Resource.Loading -> {}
        }
    }

    override suspend fun getNannyDetail(nannyId: Int): Resource<NannyProfile> {
        val result = safeApiCall { api.getNannyDetail(nannyId) }
        if (result is Resource.Success) nannyProfileDao.upsert(result.data.toEntity())
        return result.map { it.toDomain() }
    }

    override suspend fun getNannyReviews(nannyId: Int): Resource<List<Review>> =
        safeApiCall { api.getNannyReviews(nannyId) }.map { list -> list.map { it.toDomain() } }

    override suspend fun updateOwnProfile(profile: NannyProfile): Resource<NannyProfile> {
        val dto = com.nannyapp.data.api.dto.UpdateNannyProfileRequestDto(
            bio = profile.bio, experienceYears = profile.experienceYears, hourlyRate = profile.hourlyRate,
            location = profile.location, skills = profile.skills.joinToString(","),
            languages = profile.languages.joinToString(","), qualifications = profile.qualifications,
            specialisations = profile.specialisations.joinToString(","), availability = profile.availabilityText,
        )
        return safeApiCall { api.updateOwnProfile(dto) }.map { it.toDomain() }
    }

    override suspend fun getAvailability(nannyId: Int): Resource<List<DayAvailability>> =
        safeApiCall { api.getAvailability(nannyId) }.map { list -> list.map { it.toDomain() } }

    override suspend fun setAvailability(days: List<DayAvailability>): Resource<Unit> =
        safeApiCall { api.setAvailability(days.map { it.toDto() }) }

    override suspend fun getPortfolio(nannyId: Int): Resource<List<PortfolioItem>> =
        safeApiCall { api.getPortfolio(nannyId) }.map { list -> list.map { it.toDomain() } }

    override suspend fun uploadPortfolioItem(type: PortfolioType, title: String, bytes: ByteArray, fileName: String): Resource<PortfolioItem> {
        val typePart = type.name.lowercase().toRequestBody("text/plain".toMediaTypeOrNull())
        val titlePart = title.toRequestBody("text/plain".toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", fileName, bytes.toRequestBody("application/octet-stream".toMediaTypeOrNull()))
        return safeApiCall { api.uploadPortfolio(typePart, titlePart, filePart) }.map { it.toDomain() }
    }

    override suspend fun deletePortfolioItem(itemId: Int): Resource<Unit> =
        safeApiCall { api.deletePortfolio(itemId) }

    override suspend fun getEarnings(): Resource<EarningsSummary> =
        safeApiCall { api.getEarnings() }.map { it.toDomain() }
}
