package com.nannyapp.data.api

import com.nannyapp.data.api.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ReviewApi {
    @POST("reviews/create.php")
    suspend fun submitReview(@Body body: SubmitReviewRequestDto): Response<ApiEnvelope<ReviewDto>>

    @GET("reviews/list.php")
    suspend fun getReviewsForNanny(@Query("nanny_id") nannyId: Int): Response<ApiEnvelope<List<ReviewDto>>>
}
