package com.nannyapp.data.api.dto

data class ChildDto(
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

data class ReviewDto(
    val id: Int,
    val bookingId: Int,
    val reviewerId: Int,
    val reviewerName: String?,
    val nannyId: Int,
    val rating: Int,
    val comment: String?,
    val createdAt: String,
)

data class SubmitReviewRequestDto(val bookingId: Int, val nannyId: Int, val rating: Int, val comment: String)

data class ChatMessageDto(
    val id: Int,
    val senderId: Int,
    val receiverId: Int,
    val content: String,
    val isRead: Boolean,
    val createdAt: String,
)

data class ConversationDto(
    val withUserId: Int,
    val withUserName: String,
    val withUserPhotoUrl: String?,
    val lastMessage: String,
    val lastMessageAt: String,
    val unreadCount: Int,
)

data class SendMessageRequestDto(val receiverId: Int, val content: String)

data class NotificationDto(
    val id: Int,
    val title: String,
    val message: String,
    val url: String?,
    val isRead: Boolean,
    val createdAt: String,
)

data class SavedNannyDto(val id: Int, val nannyId: Int, val nanny: NannyProfileDto?, val createdAt: String)

data class SupportTicketDto(
    val id: Int,
    val userId: Int?,
    val name: String,
    val email: String,
    val category: String,
    val subject: String,
    val message: String,
    val status: String,
    val adminNotes: String?,
    val createdAt: String,
    val updatedAt: String?,
)

data class SubmitTicketRequestDto(
    val category: String,
    val subject: String,
    val message: String,
    val name: String,
    val email: String,
)

/** `blocks` is the website page's structure; absent when an admin has published plain replacement text. */
data class PageContentDto(val pageKey: String, val title: String?, val body: String?, val blocks: List<PageBlockDto>? = null)

data class PageBlockDto(val type: String?, val text: String?, val blocks: List<PageBlockDto>? = null)

/** POST reports/create.php. bookingId links the report to a booking both people share. */
data class ReportRequestDto(val reportedUserId: Int, val bookingId: Int?, val reason: String, val details: String)

data class CreatedIdDto(val id: Int)

/** One South African public holiday, from content/holidays.php. */
data class HolidayDto(val date: String, val name: String)

/** content/home.php: what the website's home page shows. Image URLs are absolute. */
data class HomeContentDto(
    val galleryImages: List<String>? = null,
    val topNannies: List<HomeNannyDto>? = null,
    val testimonials: List<TestimonialDto>? = null,
)

data class HomeNannyDto(
    val userId: Int, val fullName: String, val photoUrl: String?, val location: String?,
    val hourlyRate: Double, val averageRating: Double, val reviewCount: Int, val experienceYears: Int,
)

data class TestimonialDto(val parentName: String, val nannyName: String, val comment: String, val rating: Int)

/** subscriptions/status.php and subscriptions/verify.php. accountReference is attached to Play purchases. */
data class PlusStatusDto(
    val active: Boolean,
    val status: String?,
    val expiresAt: String?,
    val productId: String?,
    val configured: Boolean,
    val accountReference: String?,
)

data class PlusVerifyRequestDto(val productId: String, val purchaseToken: String)

/** content/languages.php: recommended is true for South Africa's official languages. */
data class LanguageDto(val code: String, val name: String, val nativeName: String, val recommended: Boolean)

data class TranslateRequestDto(val text: String, val target: String)

data class TranslatedDto(val translated: String, val provider: String?)

/** nannies/viewers.php: recent is empty and locked is true without Nanny-App Plus. */
data class ProfileViewersDto(val viewsLast30Days: Int, val locked: Boolean, val recent: List<ProfileViewerDto>?)

data class ProfileViewerDto(val userId: Int, val name: String, val photoUrl: String?, val viewedAt: String)

data class VerifiedDocumentDto(val type: String?, val title: String?, val addedAt: String?)
