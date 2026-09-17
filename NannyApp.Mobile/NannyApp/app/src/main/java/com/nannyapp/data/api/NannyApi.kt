package com.nannyapp.data.api

import com.nannyapp.data.api.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface NannyApi {
    @GET("nannies/search.php")
    suspend fun searchNannies(
        @Query("q") query: String? = null,
        @Query("location") location: String? = null,
        @Query("min_rate") minRate: Double? = null,
        @Query("max_rate") maxRate: Double? = null,
        @Query("min_experience") minExperience: Int? = null,
        @Query("min_rating") minRating: Double? = null,
        @Query("verified_only") verifiedOnly: Boolean? = null,
        @Query("slot") slot: String? = null,
        @Query("sort") sort: String? = null,
    ): Response<ApiEnvelope<List<NannyProfileDto>>>

    @GET("nannies/detail.php")
    suspend fun getNannyDetail(@Query("id") nannyId: Int): Response<ApiEnvelope<NannyProfileDto>>

    @GET("nannies/reviews.php")
    suspend fun getNannyReviews(@Query("id") nannyId: Int): Response<ApiEnvelope<List<ReviewDto>>>

    @PUT("nannies/profile.php")
    suspend fun updateOwnProfile(@Body body: UpdateNannyProfileRequestDto): Response<ApiEnvelope<NannyProfileDto>>

    @GET("nannies/availability.php")
    suspend fun getAvailability(@Query("nanny_id") nannyId: Int): Response<ApiEnvelope<List<DayAvailabilityDto>>>

    @PUT("nannies/availability.php")
    suspend fun setAvailability(@Body body: List<DayAvailabilityDto>): Response<ApiEnvelope<Unit>>

    @GET("nannies/portfolio.php")
    suspend fun getPortfolio(@Query("nanny_id") nannyId: Int): Response<ApiEnvelope<List<PortfolioItemDto>>>

    @Multipart
    @POST("nannies/portfolio.php")
    suspend fun uploadPortfolio(
        @Part("type") type: RequestBody,
        @Part("title") title: RequestBody,
        @Part file: MultipartBody.Part,
    ): Response<ApiEnvelope<PortfolioItemDto>>

    @DELETE("nannies/portfolio.php")
    suspend fun deletePortfolio(@Query("id") itemId: Int): Response<ApiEnvelope<Unit>>

    @GET("nannies/earnings.php")
    suspend fun getEarnings(): Response<ApiEnvelope<EarningsSummaryDto>>

    @GET("nannies/saved.php")
    suspend fun getSaved(): Response<ApiEnvelope<List<SavedNannyDto>>>

    @POST("nannies/save.php")
    suspend fun toggleSave(@Body body: Map<String, Int>): Response<ApiEnvelope<Map<String, Boolean>>>
}
