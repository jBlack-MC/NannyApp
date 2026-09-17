package com.nannyapp.domain.model

/**
 * Domain models used throughout the UI/ViewModel layer. These are mapped from
 * network DTOs (data.api.dto) and/or Room entities (data.db.entity) so that
 * UI code never depends directly on either transport format.
 */

data class User(
    val id: Int,
    val fullName: String,
    val email: String,
    val phone: String?,
    val role: UserRole,
    val status: AccountStatus,
    val emailVerified: Boolean,
    val profileImageUrl: String?,
    val dateOfBirth: String?,
    val address: String?,
    val gender: Gender,
    val createdAt: String?,
)

data class ParentProfile(
    val userId: Int,
    val emergencyContact: String?,
    val emergencyContactName: String?,
    val emergencyContactRelationship: String?,
    val numberOfChildren: Int,
)

data class NannyProfile(
    val userId: Int,
    val fullName: String,
    val profileImageUrl: String?,
    val bannerImageUrl: String?,
    val bio: String?,
    val gender: Gender,
    val experienceYears: Int,
    val hourlyRate: Double,
    val location: String?,
    val skills: List<String>,
    val languages: List<String>,
    val qualifications: String?,
    val specialisations: List<String>,
    val availabilityText: String?,
    val verificationStatus: VerificationStatus,
    val averageRating: Double,
    val reviewCount: Int,
    val profileViews: Int,
    val memberSince: String?,
) {
    val isVerified: Boolean get() = verificationStatus == VerificationStatus.VERIFIED
}

data class Child(
    val id: Int,
    val parentId: Int,
    val name: String,
    val age: Int?,
    val gender: String?,
    val allergies: String?,
    val medicalConditions: String?,
    val specialNeeds: String?,
    val favouriteActivities: String?,
    val notesForNannies: String?,
)

data class Booking(
    val id: Int,
    val bookingRef: String?,
    val parentId: Int,
    val parentName: String?,
    val nannyId: Int,
    val nannyName: String?,
    val nannyPhotoUrl: String? = null,
    val dateTime: String,
    val durationHours: Double,
    val location: String?,
    val notes: String?,
    val childrenDetails: String?,
    val bookingAddress: String?,
    val status: BookingStatus,
    val hourlyRate: Double = 0.0,
    val amount: Double,
    val checkInCode: String? = null, // only ever populated for the owning parent
    val checkInAttempts: Int = 0,
    val checkedInAt: String? = null,
    val checkedOutAt: String? = null,
    val parentConfirmedAt: String? = null,
    val disputeReason: String? = null,
    val disputedAt: String? = null,
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    val payoutStatus: PayoutStatus = PayoutStatus.NONE,
    val createdAt: String? = null,
) {
    val canCancel: Boolean get() = status == BookingStatus.PENDING || status == BookingStatus.CONFIRMED
    val awaitingParentConfirmation: Boolean get() = status == BookingStatus.IN_PROGRESS && checkedOutAt != null
}

data class Payment(
    val id: Int,
    val bookingId: Int,
    val bookingRef: String?,
    val amount: Double,
    val method: String,
    val transactionId: String?,
    val status: PaymentStatus,
    val payoutStatus: PayoutStatus,
    val releasedAt: String?,
    val createdAt: String?,
)

data class ChatMessage(
    val id: Int,
    val senderId: Int,
    val receiverId: Int,
    val content: String,
    val isRead: Boolean,
    val createdAt: String,
    val isMine: Boolean = false,
    val pending: Boolean = false, // optimistic local state while sending
)

data class Conversation(
    val withUserId: Int,
    val withUserName: String,
    val withUserPhotoUrl: String?,
    val lastMessage: String,
    val lastMessageAt: String,
    val unreadCount: Int,
)

data class Review(
    val id: Int,
    val bookingId: Int,
    val reviewerId: Int,
    val reviewerName: String?,
    val nannyId: Int,
    val rating: Int,
    val comment: String?,
    val createdAt: String,
)

data class AppNotification(
    val id: Int,
    val title: String,
    val message: String,
    val url: String?,
    val isRead: Boolean,
    val createdAt: String,
)

data class SavedNanny(
    val id: Int,
    val nannyId: Int,
    val nanny: NannyProfile?,
    val createdAt: String,
)

data class DayAvailability(
    val dayOfWeek: Int, // 0 = Sunday .. 6 = Saturday
    val isAvailable: Boolean,
    val timeStart: String,
    val timeEnd: String,
    val slots: Set<AvailabilitySlot> = emptySet(),
)

data class PortfolioItem(
    val id: Int,
    val type: PortfolioType,
    val title: String,
    val fileUrl: String,
    val adminVerified: Boolean,
    val createdAt: String,
)

data class SupportTicket(
    val id: Int,
    val userId: Int?,
    val name: String,
    val email: String,
    val category: SupportCategory,
    val subject: String,
    val message: String,
    val status: SupportStatus,
    val adminNotes: String?,
    val createdAt: String,
    val updatedAt: String?,
)

data class EarningsSummary(
    val totalEarnings: Double,
    val heldEarnings: Double,
    val releasedEarnings: Double,
    val completedJobs: Int,
    val recentPayments: List<Payment>,
    val earningsByDay: List<Pair<String, Double>>, // date label -> amount
)

data class AdminDashboardStats(
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
    val recentUsers: List<User>,
    val recentBookings: List<Booking>,
    val topEarningNannies: List<Pair<NannyProfile, Double>>,
    val revenueByDay: List<Pair<String, Double>>,
    val registrationsByDay: List<Pair<String, Int>>,
    val bookingsByStatus: Map<BookingStatus, Int>,
    val topLocations: List<Pair<String, Int>>,
)

data class NannySearchFilters(
    val query: String = "",
    val location: String? = null,
    val minRate: Double? = null,
    val maxRate: Double? = null,
    val minExperience: Int? = null,
    val minRating: Double? = null,
    val verifiedOnly: Boolean = false,
    val availableSlot: AvailabilitySlot? = null,
    val sortBy: NannySortOption = NannySortOption.RATING,
)

enum class NannySortOption { RATING, EXPERIENCE, PRICE_LOW, PRICE_HIGH }

/** In-memory state for the multi-step booking wizard (mirrors the session-backed wizard in book.php). */
data class BookingWizardState(
    val nanny: NannyProfile? = null,
    val dateTimeIso: String? = null,
    val durationHours: Double = 2.0,
    val selectedChildIds: Set<Int> = emptySet(),
    val address: String = "",
    val notes: String = "",
) {
    val estimatedAmount: Double get() = (nanny?.hourlyRate ?: 0.0) * durationHours
}
