package com.nannyapp.data.api

import com.nannyapp.data.api.dto.*
import retrofit2.Response
import retrofit2.http.*

interface PaymentApi {
    @POST("payments/initialize.php")
    suspend fun initializePayment(@Body body: Map<String, Int>): Response<ApiEnvelope<PaymentInitDto>>

    @GET("payments/verify.php")
    suspend fun verifyPayment(@Query("reference") reference: String): Response<ApiEnvelope<PaymentDto>>

    @GET("payments/list.php")
    suspend fun getPayments(): Response<ApiEnvelope<List<PaymentDto>>>
}
