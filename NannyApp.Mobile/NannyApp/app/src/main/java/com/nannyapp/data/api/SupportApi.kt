package com.nannyapp.data.api

import com.nannyapp.data.api.dto.*
import retrofit2.Response
import retrofit2.http.*

interface SupportApi {
    @POST("support/create.php")
    suspend fun submitTicket(@Body body: SubmitTicketRequestDto): Response<ApiEnvelope<SupportTicketDto>>

    @GET("support/my_tickets.php")
    suspend fun getMyTickets(): Response<ApiEnvelope<List<SupportTicketDto>>>
}
