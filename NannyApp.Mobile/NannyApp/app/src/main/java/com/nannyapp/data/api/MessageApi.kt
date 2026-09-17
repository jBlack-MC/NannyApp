package com.nannyapp.data.api

import com.nannyapp.data.api.dto.*
import retrofit2.Response
import retrofit2.http.*

interface MessageApi {
    @GET("messages/conversations.php")
    suspend fun getConversations(): Response<ApiEnvelope<List<ConversationDto>>>

    @GET("messages/thread.php")
    suspend fun getMessages(@Query("with") withUserId: Int): Response<ApiEnvelope<List<ChatMessageDto>>>

    @GET("messages/poll.php")
    suspend fun pollMessages(@Query("with") withUserId: Int, @Query("since_id") sinceId: Int): Response<ApiEnvelope<List<ChatMessageDto>>>

    @POST("messages/send.php")
    suspend fun sendMessage(@Body body: SendMessageRequestDto): Response<ApiEnvelope<ChatMessageDto>>

    @POST("messages/mark_read.php")
    suspend fun markRead(@Body body: Map<String, Int>): Response<ApiEnvelope<Unit>>
}
