package com.nannyapp.data.api

import com.nannyapp.data.api.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ChildApi {
    @GET("children/list.php")
    suspend fun getChildren(): Response<ApiEnvelope<List<ChildDto>>>

    @POST("children/create.php")
    suspend fun addChild(@Body body: ChildDto): Response<ApiEnvelope<ChildDto>>

    @PUT("children/update.php")
    suspend fun updateChild(@Body body: ChildDto): Response<ApiEnvelope<ChildDto>>

    @DELETE("children/delete.php")
    suspend fun deleteChild(@Query("id") childId: Int): Response<ApiEnvelope<Unit>>
}
