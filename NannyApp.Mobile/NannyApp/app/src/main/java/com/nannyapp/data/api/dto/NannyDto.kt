package com.nannyapp.data.api.dto

data class NannyProfileDto(
    val userId: Int,
    val fullName: String,
    val photoUrl: String?,
    val bannerImage: String?,
    val bio: String?,
    val gender: String?,
    val experienceYears: Int,
    val hourlyRate: Double,
    val location: String?,
    val skills: String?,          // comma-separated, mirrors nanny_profiles.skills
    val languages: String?,
    val qualifications: String?,
    val specialisations: String?,
    val availability: String?,
    val verificationStatus: String,
    val averageRating: Double,
    val reviewCount: Int,
    val profileViews: Int,
    val memberSince: String?,
)

data class UpdateNannyProfileRequestDto(
    val bio: String?,
    val experienceYears: Int,
    val hourlyRate: Double,
    val location: String?,
    val skills: String?,
    val languages: String?,
    val qualifications: String?,
    val specialisations: String?,
    val availability: String?,
)

data class DayAvailabilityDto(
    val dayOfWeek: Int,
    val isAvailable: Boolean,
    val timeStart: String,
    val timeEnd: String,
    val slots: List<String> = emptyList(),
)

data class PortfolioItemDto(
    val id: Int,
    val type: String,
    val title: String,
    val filePath: String,
    val adminVerified: Boolean,
    val createdAt: String,
)

data class EarningsSummaryDto(
    val totalEarnings: Double,
    val heldEarnings: Double,
    val releasedEarnings: Double,
    val completedJobs: Int,
    val recentPayments: List<PaymentDto>,
    val earningsByDay: Map<String, Double>,
)
