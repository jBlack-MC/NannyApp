package com.nannyapp.ui.theme

import androidx.compose.ui.graphics.Color

// NannyApp brand palette — warm coral primary (trust + warmth), soft teal secondary,
// cream surfaces. Chosen to read as "family-friendly premium", not clinical.
val NannyPrimary = Color(0xFFEF7A5C)
val NannyPrimaryDark = Color(0xFFC85A3E)
val NannyPrimaryContainer = Color(0xFFFFE0D4)
val NannySecondary = Color(0xFF2F9E8F)
val NannySecondaryContainer = Color(0xFFD3F3EC)
val NannyTertiary = Color(0xFFF4B942)

val NannyBackground = Color(0xFFFFFBF7)
val NannySurface = Color(0xFFFFFFFF)
val NannySurfaceVariant = Color(0xFFF4EDE7)
val NannyOnSurface = Color(0xFF2A2321)
val NannyOnSurfaceVariant = Color(0xFF6F625B)
val NannyOutline = Color(0xFFE3D8D0)

val NannySuccess = Color(0xFF2E9E5B)
val NannyWarning = Color(0xFFE0A82E)
val NannyError = Color(0xFFD64545)
val NannyInfo = Color(0xFF3E7CB1)

// Status colors mapped 1:1 to the booking status machine from the PHP backend.
val StatusPending = Color(0xFFE0A82E)
val StatusConfirmed = Color(0xFF3E7CB1)
val StatusInProgress = Color(0xFF8A5FD1)
val StatusCompleted = Color(0xFF2E9E5B)
val StatusRejected = Color(0xFFD64545)
val StatusCancelled = Color(0xFF8A8078)
val StatusDisputed = Color(0xFFB23B3B)
