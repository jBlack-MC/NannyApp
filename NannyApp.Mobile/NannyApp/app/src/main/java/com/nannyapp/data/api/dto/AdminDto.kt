package com.nannyapp.data.api.dto

data class AdminDashboardStatsDto(
    val totalUsers: Int,
    val totalParents: Int,
    val totalNannies: Int,
    val verifiedNannies: Int,
    val totalBookings: Int,
    val pendingBookings: Int,
    val totalRevenue: Double,
    val pendingVerifications: Int,
    val pendingDocuments: Int,
    val openSupportTickets: Int,
    val recentUsers: List<UserDto>,
    val recentBookings: List<BookingDto>,
    val topEarningNannies: List<TopNannyDto>,
    val revenueByDay: Map<String, Double>,
    val registrationsByDay: Map<String, Int>,
    val bookingsByStatus: Map<String, Int>,
    val topLocations: Map<String, Int>,
)

data class TopNannyDto(val nanny: NannyProfileDto, val totalEarnings: Double)

data class SuspendUserRequestDto(val userId: Int)
data class VerifyNannyRequestDto(val nannyId: Int, val notes: String?)
data class UpdateBookingStatusRequestDto(val bookingId: Int, val status: String)
data class UpdateTicketStatusRequestDto(val ticketId: Int, val status: String, val adminNotes: String?)
