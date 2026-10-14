package com.nannyapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nannyapp.domain.model.Booking
import com.nannyapp.domain.model.NannyProfile
import com.nannyapp.util.formatDateTime
import com.nannyapp.util.formatHours

/**
 * The website's .card: white surface, hairline border, large radius and a soft shadow.
 * Use this instead of Material's ElevatedCard so every screen shares one card style.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.large,
    border: BorderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, elevation = elevation, border = border, content = content)
    } else {
        Card(modifier = modifier, shape = shape, colors = colors, elevation = elevation, border = border, content = content)
    }
}

@Composable
fun NannyCard(
    nanny: NannyProfile,
    isSaved: Boolean,
    onClick: () -> Unit,
    onToggleSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column {
            Box {
                // The tinted panel and icon stay visible when a nanny has no photo or it fails to load.
                Box(
                    Modifier.fillMaxWidth().height(168.dp).background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                    if (!nanny.profileImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = nanny.profileImageUrl,
                            contentDescription = nanny.fullName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                IconButton(
                    onClick = onToggleSave,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape),
                ) {
                    Icon(
                        if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = if (isSaved) "Unsave" else "Save",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(nanny.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(6.dp))
                    VerificationBadge(nanny.isVerified)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RatingView(nanny.averageRating, nanny.reviewCount, compact = true)
                    Spacer(Modifier.width(10.dp))
                    Text("${nanny.experienceYears} yrs exp", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!nanny.location.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(2.dp))
                        Text(nanny.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (nanny.skills.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        nanny.skills.take(2).forEach { skill ->
                            SkillChip(skill, modifier = Modifier.weight(1f, fill = false))
                            Spacer(Modifier.width(6.dp))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "R${"%.0f".format(nanny.hourlyRate)}/hr",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
fun SkillChip(label: String, modifier: Modifier = Modifier) {
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
fun BookingCard(
    booking: Booking,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    counterpartLabel: String = "Nanny",
) {
    AppCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!booking.nannyPhotoUrl.isNullOrBlank()) {
                        AsyncImage(model = booking.nannyPhotoUrl, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(booking.nannyName ?: counterpartLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(booking.bookingRef ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                BookingStatusChip(booking.status)
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDateTime(booking.dateTime), style = MaterialTheme.typography.bodySmall)
                Text("${formatHours(booking.durationHours)}h · R${"%.0f".format(booking.amount)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    AppCard(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.height(6.dp))
            }
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
