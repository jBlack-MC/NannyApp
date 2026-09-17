package com.nannyapp.data.api.dto

data class BookingDto(
    val id: Int,
    val bookingRef: String?,
    val parentId: Int,
    val parentName: String?,
    val nannyId: Int,
    val nannyName: String?,
    val nannyPhotoUrl: String?,
    val dateTime: String,
    val duration: Double,
    val location: String?,
    val notes: String?,
    val childrenDetails: String?,
    val bookingAddress: String?,
    val status: String,
    val hourlyRate: Double,
    val amount: Double,
    val checkInCode: String?,
    val checkInAttempts: Int,
    val checkedInAt: String?,
    val checkedOutAt: String?,
    val parentConfirmedAt: String?,
    val disputeReason: String?,
    val disputedAt: String?,
    val paymentStatus: String?,
    val payoutStatus: String?,
    val createdAt: String?,
)

data class CreateBookingRequestDto(
    val nannyId: Int,
    val dateTime: String,
    val duration: Double,
    val address: String,
    val childrenIds: List<Int>,
    val childrenDetails: String?,
    val notes: String?,
)

data class CheckInRequestDto(val bookingId: Int, val checkInCode: String)
data class BookingActionRequestDto(val bookingId: Int)
data class DisputeRequestDto(val bookingId: Int, val reason: String)
data class RescheduleRequestDto(val bookingId: Int, val dateTime: String)

data class PaymentDto(
    val id: Int,
    val bookingId: Int,
    val bookingRef: String?,
    val amount: Double,
    val method: String,
    val transactionId: String?,
    val status: String,
    val payoutStatus: String?,
    val releasedAt: String?,
    val createdAt: String?,
)

data class PaymentInitDto(val authorizationUrl: String, val reference: String, val accessCode: String?)
