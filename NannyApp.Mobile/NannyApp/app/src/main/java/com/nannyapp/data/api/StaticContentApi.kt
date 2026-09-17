package com.nannyapp.data.api

import com.nannyapp.data.api.dto.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface StaticContentApi {
    @GET("content/page.php")
    suspend fun getPage(@Query("key") pageKey: String): Response<ApiEnvelope<PageContentDto>>
}
