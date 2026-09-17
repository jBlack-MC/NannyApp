package com.nannyapp.domain.repository

import com.nannyapp.domain.model.*
import com.nannyapp.util.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Repository contracts consumed by ViewModels. Implementations live in
 * data.repository and combine Retrofit (source of truth) with Room (cache)
 * per the MVVM + repository pattern requested.
 */

interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun login(email: String, password: String, rememberMe: Boolean): Resource<User>
    suspend fun register(request: RegisterData): Resource<User>
    suspend fun logout()
    suspend fun forgotPassword(email: String): Resource<Unit>
    suspend fun resetPassword(token: String, newPassword: String): Resource<Unit>
    suspend fun resendVerification(email: String): Resource<Unit>
    suspend fun verifyEmail(token: String): Resource<Unit>
    suspend fun isLoggedIn(): Boolean
}

data class RegisterData(
    val role: UserRole,
    val fullName: String,
    val email: String,
    val phone: String,
    val password: String,
    val dateOfBirth: String?,
    val address: String?,
    val gender: Gender,
    // parent-only
    val emergencyContact: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactRelationship: String? = null,
    val numberOfChildren: Int? = null,
    // nanny-only
    val bio: String? = null,
    val experienceYears: Int? = null,
    val hourlyRate: Double? = null,
    val location: String? = null,
    val skills: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val qualifications: String? = null,
    val specialisations: List<String> = emptyList(),
)

interface UserRepository {
    suspend fun getProfile(): Resource<User>
    suspend fun updateProfile(user: User): Resource<User>
    suspend fun changePassword(current: String, new: String): Resource<Unit>
    suspend fun uploadProfileImage(bytes: ByteArray, fileName: String): Resource<String>
    fun profileCompletionPercent(user: User, extra: Map<String, Boolean> = emptyMap()): Int
}

interface NannyRepository {
    fun searchNannies(filters: NannySearchFilters): Flow<Resource<List<NannyProfile>>>
    suspend fun getNannyDetail(nannyId: Int): Resource<NannyProfile>
    suspend fun getNannyReviews(nannyId: Int): Resource<List<Review>>
    suspend fun updateOwnProfile(profile: NannyProfile): Resource<NannyProfile>
    suspend fun getAvailability(nannyId: Int): Resource<List<DayAvailability>>
    suspend fun setAvailability(days: List<DayAvailability>): Resource<Unit>
    suspend fun getPortfolio(nannyId: Int): Resource<List<PortfolioItem>>
    suspend fun uploadPortfolioItem(type: PortfolioType, title: String, bytes: ByteArray, fileName: String): Resource<PortfolioItem>
    suspend fun deletePortfolioItem(itemId: Int): Resource<Unit>
    suspend fun getEarnings(): Resource<EarningsSummary>
}

interface SavedNannyRepository {
    fun getSavedNannies(): Flow<Resource<List<SavedNanny>>>
    suspend fun toggleSave(nannyId: Int, save: Boolean): Resource<Boolean>
}

interface ChildRepository {
    fun getChildren(): Flow<Resource<List<Child>>>
    suspend fun addChild(child: Child): Resource<Child>
    suspend fun updateChild(child: Child): Resource<Child>
    suspend fun deleteChild(childId: Int): Resource<Unit>
}

interface BookingRepository {
    fun getBookings(role: UserRole): Flow<Resource<List<Booking>>>
    suspend fun getBookingDetail(bookingId: Int): Resource<Booking>
    suspend fun createBooking(wizard: BookingWizardState): Resource<Booking>
    suspend fun cancelBooking(bookingId: Int): Resource<Unit>
    suspend fun rescheduleBooking(bookingId: Int, newDateTimeIso: String): Resource<Booking>
    // nanny actions
    suspend fun acceptBooking(bookingId: Int): Resource<Booking>
    suspend fun rejectBooking(bookingId: Int): Resource<Unit>
    suspend fun checkIn(bookingId: Int, pin: String): Resource<Booking>
    suspend fun checkOut(bookingId: Int): Resource<Booking>
    // parent actions
    suspend fun confirmCompletion(bookingId: Int): Resource<Booking>
    suspend fun disputeBooking(bookingId: Int, reason: String): Resource<Unit>
}

interface PaymentRepository {
    suspend fun initializePayment(bookingId: Int): Resource<PaymentInit>
    suspend fun verifyPayment(reference: String): Resource<Payment>
    fun getPaymentsForRole(): Flow<Resource<List<Payment>>>
}

data class PaymentInit(val authorizationUrl: String, val reference: String, val accessCode: String?)

interface ReviewRepository {
    suspend fun submitReview(bookingId: Int, nannyId: Int, rating: Int, comment: String): Resource<Review>
    suspend fun getReviewsForNanny(nannyId: Int): Resource<List<Review>>
}

interface MessageRepository {
    fun getConversations(): Flow<Resource<List<Conversation>>>
    fun getMessages(withUserId: Int): Flow<Resource<List<ChatMessage>>>
    suspend fun sendMessage(toUserId: Int, content: String): Resource<ChatMessage>
    suspend fun pollNewMessages(withUserId: Int, sinceId: Int): Resource<List<ChatMessage>>
    suspend fun markConversationRead(withUserId: Int)
}

interface NotificationRepository {
    fun getNotifications(): Flow<Resource<List<AppNotification>>>
    fun unreadCount(): Flow<Int>
    suspend fun markRead(notificationId: Int)
    suspend fun markAllRead()
    suspend fun refresh(): Resource<Unit>
}

interface SupportRepository {
    suspend fun submitTicket(category: SupportCategory, subject: String, message: String, name: String, email: String): Resource<SupportTicket>
    fun getMyTickets(): Flow<Resource<List<SupportTicket>>>
}

interface AdminRepository {
    suspend fun getDashboardStats(): Resource<AdminDashboardStats>
    fun getUsers(roleFilter: UserRole?, query: String): Flow<Resource<List<User>>>
    suspend fun suspendUser(userId: Int): Resource<Unit>
    suspend fun activateUser(userId: Int): Resource<Unit>
    fun getPendingVerifications(): Flow<Resource<List<NannyProfile>>>
    suspend fun verifyPortfolioItem(itemId: Int, approve: Boolean, notes: String?): Resource<Unit>
    suspend fun approveNanny(nannyId: Int, notes: String?): Resource<Unit>
    suspend fun rejectNanny(nannyId: Int, notes: String?): Resource<Unit>
    fun getAllBookings(statusFilter: BookingStatus?, query: String): Flow<Resource<List<Booking>>>
    suspend fun updateBookingStatus(bookingId: Int, status: BookingStatus): Resource<Unit>
    fun getAllPayments(statusFilter: PaymentStatus?): Flow<Resource<List<Payment>>>
    suspend fun refundPayment(paymentId: Int): Resource<Unit>
    fun getAllSupportTickets(statusFilter: SupportStatus?): Flow<Resource<List<SupportTicket>>>
    suspend fun updateTicketStatus(ticketId: Int, status: SupportStatus, adminNotes: String?): Resource<Unit>
}

interface StaticContentRepository {
    suspend fun getPage(pageKey: String): Resource<Pair<String, String>> // title to HTML/plain body
}
