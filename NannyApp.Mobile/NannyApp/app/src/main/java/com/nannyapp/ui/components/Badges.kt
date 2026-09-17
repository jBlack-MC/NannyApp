package com.nannyapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nannyapp.domain.model.BookingStatus
import com.nannyapp.domain.model.SupportStatus
import com.nannyapp.ui.theme.*

@Composable
fun VerificationBadge(verified: Boolean, modifier: Modifier = Modifier) {
    if (!verified) return
    Row(
        modifier = modifier
            .background(NannySecondaryContainer, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Verified, contentDescription = "Verified", tint = NannySecondary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text("Verified", fontSize = 11.sp, color = NannySecondary)
    }
}

@Composable
fun RatingView(rating: Double, reviewCount: Int? = null, modifier: Modifier = Modifier, compact: Boolean = false) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = NannyTertiary, modifier = Modifier.size(if (compact) 14.dp else 18.dp))
        Spacer(Modifier.width(2.dp))
        Text(
            text = if (rating > 0) "%.1f".format(rating) else "New",
            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleSmall,
        )
        if (reviewCount != null && reviewCount > 0) {
            Spacer(Modifier.width(4.dp))
            Text("($reviewCount)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

fun bookingStatusColor(status: BookingStatus): Color = when (status) {
    BookingStatus.PENDING -> StatusPending
    BookingStatus.CONFIRMED -> StatusConfirmed
    BookingStatus.IN_PROGRESS -> StatusInProgress
    BookingStatus.COMPLETED -> StatusCompleted
    BookingStatus.REJECTED -> StatusRejected
    BookingStatus.CANCELLED -> StatusCancelled
    BookingStatus.DISPUTED -> StatusDisputed
}

@Composable
fun BookingStatusChip(status: BookingStatus, modifier: Modifier = Modifier) {
    val color = bookingStatusColor(status)
    Text(
        text = status.label,
        color = color,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun SupportStatusChip(status: SupportStatus, modifier: Modifier = Modifier) {
    val color = when (status) {
        SupportStatus.OPEN -> StatusPending
        SupportStatus.IN_PROGRESS -> StatusInProgress
        SupportStatus.RESOLVED -> StatusCompleted
        SupportStatus.CLOSED -> StatusCancelled
    }
    val label = when (status) {
        SupportStatus.OPEN -> "Open"
        SupportStatus.IN_PROGRESS -> "In progress"
        SupportStatus.RESOLVED -> "Resolved"
        SupportStatus.CLOSED -> "Closed"
    }
    Text(
        text = label, color = color, style = MaterialTheme.typography.labelMedium,
        modifier = modifier.background(color.copy(alpha = 0.12f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun UnreadDot(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(8.dp).background(MaterialTheme.colorScheme.error, CircleShape))
}
