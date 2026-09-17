package com.nannyapp.data.api

import com.nannyapp.data.api.dto.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface UserApi {
    @GET("user/profile.php")
    suspend fun getProfile(): Response<ApiEnvelope<UserDto>>

    @PUT("user/profile.php")
    suspend fun updateProfile(@Body body: UserDto): Response<ApiEnvelope<UserDto>>

    @POST("user/change_password.php")
    suspend fun changePassword(@Body body: Map<String, String>): Response<ApiEnvelope<Unit>>

    @Multipart
    @POST("user/upload_avatar.php")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): Response<ApiEnvelope<Map<String, String>>>
}
