package com.nannyapp.ui.theme

import androidx.compose.ui.graphics.Color

// NannyApp brand palette — professional trust blue palette from the new design.
// Mirrors the logo-derived palette in NannyApp.Web/assets/css/style.css.
val NannyPrimary = Color(0xFF1E5F9E)
val NannyPrimaryDark = Color(0xFF123F70)
val NannyPrimaryContainer = Color(0xFFD9EFFF)
val NannySecondary = Color(0xFF3B9BE0)
val NannySecondaryContainer = Color(0xFFEEF7FF)
val NannyTertiary = Color(0xFF27B37E)

val NannyBackground = Color(0xFFF3F8FD)
val NannySurface = Color(0xFFFFFFFF)
val NannySurfaceVariant = Color(0xFFEEF7FF)
val NannyOnSurface = Color(0xFF0D1F33)
val NannyOnSurfaceVariant = Color(0xFF5E7387)
val NannyOutline = Color(0xFFE2E8F0)
val NannyInkSoft = Color(0xFF2D465F)
// Ends of the website's primary button gradient (--blue-700 to --blue-500).
val NannyGradientStart = Color(0xFF1E5F9E)
val NannyGradientEnd = Color(0xFF3B9BE0)

val NannySuccess = Color(0xFF4CAF50)
val NannyWarning = Color(0xFFFFA000)
val NannyError = Color(0xFFD32F2F)
val NannyInfo = Color(0xFF0288D1)

// Status colors mapped 1:1 to the booking status machine from the PHP backend.
// Each is dark enough (4.5:1 or better) to read as text on its own pale tint.
val StatusPending = Color(0xFF92400E)
val StatusConfirmed = Color(0xFF0369A1)
val StatusInProgress = Color(0xFF6D28D9)
val StatusCompleted = Color(0xFF047857)
val StatusRejected = Color(0xFFB91C1C)
val StatusCancelled = Color(0xFF475569)
val StatusDisputed = Color(0xFF991B1B)
// The website's rating-star amber.
val NannyStar = Color(0xFFF5A623)
