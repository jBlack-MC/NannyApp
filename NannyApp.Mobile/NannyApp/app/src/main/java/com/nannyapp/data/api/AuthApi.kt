package com.nannyapp.data.api

import com.nannyapp.data.api.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login.php")
    suspend fun login(@Body body: LoginRequestDto): Response<ApiEnvelope<LoginResponseDto>>

    @POST("auth/register.php")
    suspend fun register(@Body body: RegisterRequestDto): Response<ApiEnvelope<LoginResponseDto>>

    @POST("auth/logout.php")
    suspend fun logout(): Response<ApiEnvelope<Unit>>

    @POST("auth/forgot.php")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequestDto): Response<ApiEnvelope<Unit>>

    @POST("auth/reset.php")
    suspend fun resetPassword(@Body body: ResetPasswordRequestDto): Response<ApiEnvelope<Unit>>

    @POST("auth/verify-email.php")
    suspend fun verifyEmail(@Body body: VerifyEmailRequestDto): Response<ApiEnvelope<Unit>>

    @POST("auth/resend-verification.php")
    suspend fun resendVerification(@Body body: ResendVerificationRequestDto): Response<ApiEnvelope<Unit>>
}
