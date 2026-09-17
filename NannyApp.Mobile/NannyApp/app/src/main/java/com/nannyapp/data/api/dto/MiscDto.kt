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

data class PageContentDto(val pageKey: String, val title: String?, val body: String?)
