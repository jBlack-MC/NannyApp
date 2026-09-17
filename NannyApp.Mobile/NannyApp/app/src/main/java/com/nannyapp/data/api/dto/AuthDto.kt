package com.nannyapp.data.api.dto

data class LoginRequestDto(val email: String, val password: String, val rememberMe: Boolean)
data class LoginResponseDto(val token: String, val user: UserDto)

data class RegisterRequestDto(
    val role: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val password: String,
    val dateOfBirth: String?,
    val address: String?,
    val gender: String?,
    val emergencyContact: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactRelationship: String? = null,
    val numberOfChildren: Int? = null,
    val bio: String? = null,
    val experienceYears: Int? = null,
    val hourlyRate: Double? = null,
    val location: String? = null,
    val skills: String? = null,
    val languages: String? = null,
    val qualifications: String? = null,
    val specialisations: String? = null,
)

data class ForgotPasswordRequestDto(val email: String)
data class ResetPasswordRequestDto(val token: String, val newPassword: String)
data class VerifyEmailRequestDto(val token: String)
data class ResendVerificationRequestDto(val email: String)

data class UserDto(
    val id: Int,
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
