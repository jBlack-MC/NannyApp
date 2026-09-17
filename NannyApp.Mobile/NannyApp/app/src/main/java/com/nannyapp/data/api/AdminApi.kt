package com.nannyapp.data.api

import com.nannyapp.data.api.dto.*
import retrofit2.Response
import retrofit2.http.*

interface AdminApi {
    @GET("admin/dashboard.php")
    suspend fun getDashboardStats(): Response<ApiEnvelope<AdminDashboardStatsDto>>

    @GET("admin/users.php")
    suspend fun getUsers(@Query("role") role: String? = null, @Query("q") query: String? = null): Response<ApiEnvelope<List<UserDto>>>

    @POST("admin/users_suspend.php")
    suspend fun suspendUser(@Body body: SuspendUserRequestDto): Response<ApiEnvelope<Unit>>

    @POST("admin/users_activate.php")
    suspend fun activateUser(@Body body: SuspendUserRequestDto): Response<ApiEnvelope<Unit>>

    @GET("admin/verifications.php")
    suspend fun getPendingVerifications(): Response<ApiEnvelope<List<NannyProfileDto>>>

    @POST("admin/verify_nanny.php")
    suspend fun approveNanny(@Body body: VerifyNannyRequestDto): Response<ApiEnvelope<Unit>>

    @POST("admin/reject_nanny.php")
    suspend fun rejectNanny(@Body body: VerifyNannyRequestDto): Response<ApiEnvelope<Unit>>

    @POST("admin/verify_document.php")
    suspend fun verifyDocument(@Body body: Map<String, Any>): Response<ApiEnvelope<Unit>>

    @GET("admin/bookings.php")
    suspend fun getAllBookings(@Query("status") status: String? = null, @Query("q") query: String? = null): Response<ApiEnvelope<List<BookingDto>>>

    @POST("admin/booking_status.php")
    suspend fun updateBookingStatus(@Body body: UpdateBookingStatusRequestDto): Response<ApiEnvelope<Unit>>

    @GET("admin/payments.php")
    suspend fun getAllPayments(@Query("status") status: String? = null): Response<ApiEnvelope<List<PaymentDto>>>

    @POST("admin/refund.php")
    suspend fun refundPayment(@Body body: Map<String, Int>): Response<ApiEnvelope<Unit>>

    @GET("admin/support.php")
    suspend fun getAllTickets(@Query("status") status: String? = null): Response<ApiEnvelope<List<SupportTicketDto>>>

    @POST("admin/support_update.php")
    suspend fun updateTicketStatus(@Body body: UpdateTicketStatusRequestDto): Response<ApiEnvelope<Unit>>
}
