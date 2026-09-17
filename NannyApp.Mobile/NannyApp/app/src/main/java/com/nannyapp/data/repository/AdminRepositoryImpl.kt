package com.nannyapp.data.repository

import com.nannyapp.data.api.AdminApi
import com.nannyapp.data.api.dto.*
import com.nannyapp.domain.model.*
import com.nannyapp.domain.repository.AdminRepository
import com.nannyapp.util.Resource
import com.nannyapp.util.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepositoryImpl @Inject constructor(private val api: AdminApi) : AdminRepository {

    override suspend fun getDashboardStats(): Resource<AdminDashboardStats> =
        safeApiCall { api.getDashboardStats() }.map { it.toDomain() }

    override fun getUsers(roleFilter: UserRole?, query: String): Flow<Resource<List<User>>> = flow {
        emit(Resource.Loading)
        emit(safeApiCall { api.getUsers(roleFilter?.toApi(), query.ifBlank { null }) }.map { list -> list.map { it.toDomain() } })
    }

    override suspend fun suspendUser(userId: Int): Resource<Unit> =
        safeApiCall { api.suspendUser(SuspendUserRequestDto(userId)) }

    override suspend fun activateUser(userId: Int): Resource<Unit> =
        safeApiCall { api.activateUser(SuspendUserRequestDto(userId)) }

    override fun getPendingVerifications(): Flow<Resource<List<NannyProfile>>> = flow {
        emit(Resource.Loading)
        emit(safeApiCall { api.getPendingVerifications() }.map { list -> list.map { it.toDomain() } })
    }

    override suspend fun verifyPortfolioItem(itemId: Int, approve: Boolean, notes: String?): Resource<Unit> =
        safeApiCall { api.verifyDocument(mapOf("id" to itemId, "approve" to approve, "notes" to (notes ?: ""))) }

    override suspend fun approveNanny(nannyId: Int, notes: String?): Resource<Unit> =
        safeApiCall { api.approveNanny(VerifyNannyRequestDto(nannyId, notes)) }

    override suspend fun rejectNanny(nannyId: Int, notes: String?): Resource<Unit> =
        safeApiCall { api.rejectNanny(VerifyNannyRequestDto(nannyId, notes)) }

    override fun getAllBookings(statusFilter: BookingStatus?, query: String): Flow<Resource<List<Booking>>> = flow {
        emit(Resource.Loading)
        emit(safeApiCall { api.getAllBookings(statusFilter?.toApi(), query.ifBlank { null }) }.map { list -> list.map { it.toDomain() } })
    }

    override suspend fun updateBookingStatus(bookingId: Int, status: BookingStatus): Resource<Unit> =
        safeApiCall { api.updateBookingStatus(UpdateBookingStatusRequestDto(bookingId, status.toApi())) }

    override fun getAllPayments(statusFilter: PaymentStatus?): Flow<Resource<List<Payment>>> = flow {
        emit(Resource.Loading)
        val statusStr = statusFilter?.name?.lowercase()
        emit(safeApiCall { api.getAllPayments(statusStr) }.map { list -> list.map { it.toDomain() } })
    }

    override suspend fun refundPayment(paymentId: Int): Resource<Unit> =
        safeApiCall { api.refundPayment(mapOf("payment_id" to paymentId)) }

    override fun getAllSupportTickets(statusFilter: SupportStatus?): Flow<Resource<List<SupportTicket>>> = flow {
        emit(Resource.Loading)
        val statusStr = statusFilter?.name?.lowercase()
        emit(safeApiCall { api.getAllTickets(statusStr) }.map { list -> list.map { it.toDomain() } })
    }

    override suspend fun updateTicketStatus(ticketId: Int, status: SupportStatus, adminNotes: String?): Resource<Unit> =
        safeApiCall { api.updateTicketStatus(UpdateTicketStatusRequestDto(ticketId, status.name.lowercase(), adminNotes)) }
}
