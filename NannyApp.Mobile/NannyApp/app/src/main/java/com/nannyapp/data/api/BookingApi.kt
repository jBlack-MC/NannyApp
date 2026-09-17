package com.nannyapp.data.api

import com.nannyapp.data.api.dto.*
import retrofit2.Response
import retrofit2.http.*

interface BookingApi {
    @GET("bookings/list.php")
    suspend fun getBookings(): Response<ApiEnvelope<List<BookingDto>>>

    @GET("bookings/detail.php")
    suspend fun getBookingDetail(@Query("id") bookingId: Int): Response<ApiEnvelope<BookingDto>>

    @POST("bookings/create.php")
    suspend fun createBooking(@Body body: CreateBookingRequestDto): Response<ApiEnvelope<BookingDto>>

    @POST("bookings/cancel.php")
    suspend fun cancelBooking(@Body body: BookingActionRequestDto): Response<ApiEnvelope<Unit>>

    @POST("bookings/reschedule.php")
    suspend fun rescheduleBooking(@Body body: RescheduleRequestDto): Response<ApiEnvelope<BookingDto>>

    @POST("bookings/accept.php")
    suspend fun acceptBooking(@Body body: BookingActionRequestDto): Response<ApiEnvelope<BookingDto>>

    @POST("bookings/reject.php")
    suspend fun rejectBooking(@Body body: BookingActionRequestDto): Response<ApiEnvelope<Unit>>

    @POST("bookings/check_in.php")
    suspend fun checkIn(@Body body: CheckInRequestDto): Response<ApiEnvelope<BookingDto>>

    @POST("bookings/check_out.php")
    suspend fun checkOut(@Body body: BookingActionRequestDto): Response<ApiEnvelope<BookingDto>>

    @POST("bookings/confirm.php")
    suspend fun confirmCompletion(@Body body: BookingActionRequestDto): Response<ApiEnvelope<BookingDto>>

    @POST("bookings/dispute.php")
    suspend fun disputeBooking(@Body body: DisputeRequestDto): Response<ApiEnvelope<Unit>>
}
