package com.nannyapp.data.repository

import com.nannyapp.data.api.ReviewApi
import com.nannyapp.data.api.dto.SubmitReviewRequestDto
import com.nannyapp.data.db.dao.ReviewDao
import com.nannyapp.domain.model.*
import com.nannyapp.domain.repository.ReviewRepository
import com.nannyapp.util.Resource
import com.nannyapp.util.safeApiCall
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepositoryImpl @Inject constructor(
    private val api: ReviewApi,
    private val dao: ReviewDao,
) : ReviewRepository {

    override suspend fun submitReview(bookingId: Int, nannyId: Int, rating: Int, comment: String): Resource<Review> {
        val result = safeApiCall { api.submitReview(SubmitReviewRequestDto(bookingId, nannyId, rating, comment)) }
        if (result is Resource.Success) {
            dao.upsert(result.data.toEntity())
        }
        return result.map { it.asDomain() }
    }

    override suspend fun getReviewsForNanny(nannyId: Int): Resource<List<Review>> {
        val result = safeApiCall { api.getReviewsForNanny(nannyId) }
        return when (result) {
            is Resource.Success -> {
                dao.upsertAll(result.data.map { it.toEntity() })
                Resource.Success(result.data.map { it.asDomain() })
            }
            is Resource.Error -> {
                val cached = dao.observeForNanny(nannyId).first()
                if (cached.isNotEmpty()) Resource.Success(cached.map { it.asDomain() }) else result
            }
            Resource.Loading -> Resource.Loading
        }
    }
}

