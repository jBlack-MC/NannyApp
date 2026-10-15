package com.nannyapp.data.api

import com.nannyapp.data.api.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** Nanny-App Plus, profile viewers and machine translation. */
interface PlusApi {
    @GET("subscriptions/status.php")
    suspend fun status(): Response<ApiEnvelope<PlusStatusDto>>

    @POST("subscriptions/verify.php")
    suspend fun verify(@Body body: PlusVerifyRequestDto): Response<ApiEnvelope<PlusStatusDto>>

    @GET("nannies/viewers.php")
    suspend fun viewers(): Response<ApiEnvelope<ProfileViewersDto>>

    @GET("content/languages.php")
    suspend fun languages(): Response<ApiEnvelope<List<LanguageDto>>>

    @POST("content/translate.php")
    suspend fun translate(@Body body: TranslateRequestDto): Response<ApiEnvelope<TranslatedDto>>
}
