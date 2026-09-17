package com.nannyapp.data.repository

import com.nannyapp.data.api.ReviewApi
import com.nannyapp.data.api.dto.SubmitReviewRequestDto
import com.nannyapp.domain.model.Review
import com.nannyapp.domain.repository.ReviewRepository
import com.nannyapp.util.Resource
import com.nannyapp.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepositoryImpl @Inject constructor(private val api: ReviewApi) : ReviewRepository {

    override suspend fun submitReview(bookingId: Int, nannyId: Int, rating: Int, comment: String): Resource<Review> =
        safeApiCall { api.submitReview(SubmitReviewRequestDto(bookingId, nannyId, rating, comment)) }.map { it.toDomain() }

    override suspend fun getReviewsForNanny(nannyId: Int): Resource<List<Review>> =
        safeApiCall { api.getReviewsForNanny(nannyId) }.map { list -> list.map { it.toDomain() } }
}
