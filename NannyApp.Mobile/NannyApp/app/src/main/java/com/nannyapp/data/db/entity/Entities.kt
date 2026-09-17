package com.nannyapp.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entities used purely as a local cache/offline layer (request #34) —
 * NOT the source of truth. The Retrofit API + MySQL backend remain
 * authoritative; these tables are refreshed on each successful network call
 * and read from when offline.
 */

@Entity(tableName = "cached_user")
data class UserEntity(
    @PrimaryKey val id: Int,
    val fullName: String,
    val email: String,
    val phone: String?,
    val role: String,
    val status: String,
    val emailVerified: Boolean,
    val profileImage: String?,
    val dateOfBirth: String?,
    val address: String?,
    val gender: String?,
    val createdAt: String?,
)

@Entity(tableName = "cached_nanny_profile")
data class NannyProfileEntity(
    @PrimaryKey val userId: Int,
    val fullName: String,
    val photoUrl: String?,
    val bannerImage: String?,
    val bio: String?,
    val gender: String?,
    val experienceYears: Int,
    val hourlyRate: Double,
    val location: String?,
    val skills: String?,
    val languages: String?,
    val qualifications: String?,
    val specialisations: String?,
    val availability: String?,
    val verificationStatus: String,
    val averageRating: Double,
    val reviewCount: Int,
    val profileViews: Int,
    val memberSince: String?,
    val lastSyncedAt: Long,
)

@Entity(tableName = "cached_child")
data class ChildEntity(
    @PrimaryKey val id: Int,
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

@Entity(tableName = "cached_booking")
data class BookingEntity(
    @PrimaryKey val id: Int,
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

@Entity(tableName = "cached_payment")
data class PaymentEntity(
    @PrimaryKey val id: Int,
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

@Entity(tableName = "cached_review")
data class ReviewEntity(
    @PrimaryKey val id: Int,
    val bookingId: Int,
    val reviewerId: Int,
    val reviewerName: String?,
    val nannyId: Int,
    val rating: Int,
    val comment: String?,
    val createdAt: String,
)

@Entity(tableName = "cached_saved_nanny")
data class SavedNannyEntity(
    @PrimaryKey val id: Int,
    val nannyId: Int,
    val createdAt: String,
)

@Entity(tableName = "cached_message")
data class ChatMessageEntity(
    @PrimaryKey val id: Int,
    val senderId: Int,
    val receiverId: Int,
    val content: String,
    val isRead: Boolean,
    val createdAt: String,
)

@Entity(tableName = "cached_notification")
data class NotificationEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val message: String,
    val url: String?,
    val isRead: Boolean,
    val createdAt: String,
)

@Entity(tableName = "cached_availability")
data class AvailabilityEntity(
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
    val nannyId: Int,
    val dayOfWeek: Int,
    val isAvailable: Boolean,
    val timeStart: String,
    val timeEnd: String,
    val slots: String, // comma-separated AvailabilitySlot names
)
