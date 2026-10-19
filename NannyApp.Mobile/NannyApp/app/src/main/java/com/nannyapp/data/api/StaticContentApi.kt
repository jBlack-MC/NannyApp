package com.nannyapp.data.api

import com.nannyapp.data.api.dto.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface StaticContentApi {
    @GET("content/page.php")
    suspend fun getPage(@Query("key") pageKey: String): Response<ApiEnvelope<PageContentDto>>

    @GET("content/home.php")
    suspend fun getHome(): Response<ApiEnvelope<HomeContentDto>>

    @GET("content/holidays.php")
    suspend fun getHolidays(@Query("year") year: Int): Response<ApiEnvelope<List<com.nannyapp.data.api.dto.HolidayDto>>>
}
