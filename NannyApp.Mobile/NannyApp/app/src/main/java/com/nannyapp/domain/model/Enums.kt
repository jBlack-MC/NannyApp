package com.nannyapp.domain.model

enum class UserRole { PARENT, NANNY, ADMIN;
    companion object {
        fun fromApi(value: String?): UserRole = when (value?.lowercase()) {
            "nanny" -> NANNY
            "admin" -> ADMIN
            else -> PARENT
        }
    }
    fun toApi(): String = name.lowercase()
}

enum class AccountStatus { ACTIVE, SUSPENDED }

enum class Gender { MALE, FEMALE, NON_BINARY, PREFER_NOT_TO_SAY, OTHER, UNSPECIFIED;
    companion object {
        fun fromApi(value: String?): Gender = when (value) {
            "male" -> MALE
            "female" -> FEMALE
            "non-binary" -> NON_BINARY
            "prefer_not_to_say" -> PREFER_NOT_TO_SAY
            "other" -> OTHER
            else -> UNSPECIFIED
        }
    }
}

enum class VerificationStatus { PENDING, VERIFIED, REJECTED;
    companion object {
        fun fromApi(value: String?): VerificationStatus = when (value) {
            "verified" -> VERIFIED
            "rejected" -> REJECTED
            else -> PENDING
        }
    }
}

/**
 * Mirrors the exact status machine implemented in nanny/bookings.php and
 * migrate_v4.sql: pending -> confirmed -> in_progress -> completed,
 * with rejected / cancelled / disputed as terminal/side states.
 */
enum class BookingStatus {
    PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, REJECTED, CANCELLED, DISPUTED;

    companion object {
        fun fromApi(value: String?): BookingStatus = when (value) {
            "confirmed" -> CONFIRMED
            "in_progress" -> IN_PROGRESS
            "completed" -> COMPLETED
            "rejected" -> REJECTED
            "cancelled" -> CANCELLED
            "disputed" -> DISPUTED
            else -> PENDING
        }
    }

    fun toApi(): String = name.lowercase()

    val label: String get() = when (this) {
        PENDING -> "Pending"
        CONFIRMED -> "Confirmed"
        IN_PROGRESS -> "In progress"
        COMPLETED -> "Completed"
        REJECTED -> "Rejected"
        CANCELLED -> "Cancelled"
        DISPUTED -> "Disputed"
    }
}

enum class PaymentStatus { PENDING, PAID, FAILED, REFUNDED;
    companion object {
        fun fromApi(value: String?): PaymentStatus = when (value) {
            "paid" -> PAID
            "failed" -> FAILED
            "refunded" -> REFUNDED
            else -> PENDING
        }
    }
}

/** held -> released (parent confirmed, or auto-release after 48h) -> or refunded */
enum class PayoutStatus { HELD, RELEASED, REFUNDED, NONE;
    companion object {
        fun fromApi(value: String?): PayoutStatus = when (value) {
            "held" -> HELD
            "released" -> RELEASED
            "refunded" -> REFUNDED
            else -> NONE
        }
    }
}

enum class AvailabilitySlot { MORNING, AFTERNOON, EVENING;
    companion object {
        fun fromApi(value: String?): AvailabilitySlot = when (value) {
            "afternoon" -> AFTERNOON
            "evening" -> EVENING
            else -> MORNING
        }
    }
    fun toApi(): String = name.lowercase()
}

enum class PortfolioType { CERTIFICATE, ID, PHOTO, REFERENCE, OTHER;
    companion object {
        fun fromApi(value: String?): PortfolioType = when (value) {
            "id" -> ID
            "photo" -> PHOTO
            "reference" -> REFERENCE
            "other" -> OTHER
            else -> CERTIFICATE
        }
    }
}

enum class SupportCategory { BOOKING, PAYMENT, TECHNICAL, SAFETY, GENERAL;
    fun toApi(): String = name.lowercase()
    companion object {
        fun fromApi(value: String?): SupportCategory = when (value) {
            "booking" -> BOOKING
            "payment" -> PAYMENT
            "technical" -> TECHNICAL
            "safety" -> SAFETY
            else -> GENERAL
        }
    }
}

enum class SupportStatus { OPEN, IN_PROGRESS, RESOLVED, CLOSED;
    companion object {
        fun fromApi(value: String?): SupportStatus = when (value) {
            "in_progress" -> IN_PROGRESS
            "resolved" -> RESOLVED
            "closed" -> CLOSED
            else -> OPEN
        }
    }
}
