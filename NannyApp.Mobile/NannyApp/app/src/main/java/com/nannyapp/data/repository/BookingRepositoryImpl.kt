package com.nannyapp.data.repository

import com.nannyapp.data.api.BookingApi
import com.nannyapp.data.api.dto.*
import com.nannyapp.data.db.dao.BookingDao
import com.nannyapp.domain.model.*
import com.nannyapp.domain.repository.BookingRepository
import com.nannyapp.util.Resource
import com.nannyapp.util.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implements the booking lifecycle exactly as found in parent/book.php and
 * nanny/bookings.php: pending -> confirmed (accept, generates check_in_code)
 * -> in_progress (check-in with PIN) -> completed (parent confirms, releases
 * escrow) with reject/cancel/dispute as alternate paths.
 */
@Singleton
class BookingRepositoryImpl @Inject constructor(
    private val api: BookingApi,
    private val dao: BookingDao,
) : BookingRepository {

    override fun getBookings(role: UserRole): Flow<Resource<List<Booking>>> = flow {
        emit(Resource.Loading)
        when (val result = safeApiCall { api.getBookings() }) {
            is Resource.Success -> {
                dao.upsertAll(result.data.map { it.toEntity() })
                emit(Resource.Success(result.data.map { it.toDomain() }))
            }
            is Resource.Error -> {
                val cached = dao.observeAll().first()
                if (cached.isNotEmpty()) emit(Resource.Success(cached.map { it.toDomain() })) else emit(result)
            }
            Resource.Loading -> {}
        }
    }

    override suspend fun getBookingDetail(bookingId: Int): Resource<Booking> {
        val result = safeApiCall { api.getBookingDetail(bookingId) }
        if (result is Resource.Success) dao.upsert(result.data.toEntity())
        return result.map { it.toDomain() }
    }

    override suspend fun createBooking(wizard: BookingWizardState): Resource<Booking> {
        val nanny = wizard.nanny ?: return Resource.Error("Please choose a nanny first.")
        val dateTime = wizard.dateTimeIso ?: return Resource.Error("Please choose a date and time.")
        val dto = CreateBookingRequestDto(
            nannyId = nanny.userId,
            dateTime = dateTime,
            duration = wizard.durationHours,
            address = wizard.address,
            childrenIds = wizard.selectedChildIds.toList(),
            childrenDetails = null,
            notes = wizard.notes.ifBlank { null },
        )
        val result = safeApiCall { api.createBooking(dto) }
        if (result is Resource.Success) dao.upsert(result.data.toEntity())
        return result.map { it.toDomain() }
    }

    override suspend fun cancelBooking(bookingId: Int): Resource<Unit> =
        safeApiCall { api.cancelBooking(BookingActionRequestDto(bookingId)) }

    override suspend fun rescheduleBooking(bookingId: Int, newDateTimeIso: String): Resource<Booking> =
        safeApiCall { api.rescheduleBooking(RescheduleRequestDto(bookingId, newDateTimeIso)) }.map { it.toDomain() }

    override suspend fun acceptBooking(bookingId: Int): Resource<Booking> {
        val result = safeApiCall { api.acceptBooking(BookingActionRequestDto(bookingId)) }
        if (result is Resource.Success) dao.upsert(result.data.toEntity())
        return result.map { it.toDomain() }
    }

    override suspend fun rejectBooking(bookingId: Int): Resource<Unit> =
        safeApiCall { api.rejectBooking(BookingActionRequestDto(bookingId)) }

    override suspend fun checkIn(bookingId: Int, pin: String): Resource<Booking> {
        // Server enforces the 5-attempt lockout from nanny/bookings.php; we just relay its response.
        val result = safeApiCall { api.checkIn(CheckInRequestDto(bookingId, pin)) }
        if (result is Resource.Success) dao.upsert(result.data.toEntity())
        return result.map { it.toDomain() }
    }

    override suspend fun checkOut(bookingId: Int): Resource<Booking> {
        val result = safeApiCall { api.checkOut(BookingActionRequestDto(bookingId)) }
        if (result is Resource.Success) dao.upsert(result.data.toEntity())
        return result.map { it.toDomain() }
    }

    override suspend fun confirmCompletion(bookingId: Int): Resource<Booking> {
        val result = safeApiCall { api.confirmCompletion(BookingActionRequestDto(bookingId)) }
        if (result is Resource.Success) dao.upsert(result.data.toEntity())
        return result.map { it.toDomain() }
    }

    override suspend fun disputeBooking(bookingId: Int, reason: String): Resource<Unit> =
        safeApiCall { api.disputeBooking(DisputeRequestDto(bookingId, reason)) }
}
