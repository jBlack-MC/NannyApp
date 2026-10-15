package com.nannyapp.data.api

import com.nannyapp.data.api.dto.ApiEnvelope
import com.nannyapp.data.api.dto.CreatedIdDto
import com.nannyapp.data.api.dto.ReportRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ReportApi {
    @POST("reports/create.php")
    suspend fun report(@Body body: ReportRequestDto): Response<ApiEnvelope<CreatedIdDto>>
}
