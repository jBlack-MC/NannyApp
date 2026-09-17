package com.nannyapp.data.api

import com.nannyapp.data.api.dto.*
import retrofit2.Response
import retrofit2.http.*

interface NotificationApi {
    @GET("notifications/list.php")
    suspend fun getNotifications(): Response<ApiEnvelope<List<NotificationDto>>>

    @POST("notifications/mark_read.php")
    suspend fun markRead(@Body body: Map<String, Int>): Response<ApiEnvelope<Unit>>

    @POST("notifications/mark_all_read.php")
    suspend fun markAllRead(): Response<ApiEnvelope<Unit>>
}
