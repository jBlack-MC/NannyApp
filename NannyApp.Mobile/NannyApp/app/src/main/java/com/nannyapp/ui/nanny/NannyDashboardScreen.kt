package com.nannyapp.ui.nanny

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannyapp.ui.components.*
import com.nannyapp.ui.components.AppCard
import com.nannyapp.R
import androidx.compose.ui.res.stringResource

@Composable
fun NannyDashboardScreen(
    onBookingClick: (Int) -> Unit,
    onManageBookings: () -> Unit,
    onAvailability: () -> Unit,
    onEditProfile: () -> Unit,
    onEarnings: () -> Unit,
    onPortfolio: () -> Unit,
    onReviews: () -> Unit,
    onNotifications: () -> Unit,
    onPlus: () -> Unit,
    viewModel: NannyDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { AppTopBar(title = "NannyApp", unreadNotifications = state.unreadNotifications, onNotificationsClick = onNotifications) }) { padding ->
        when {
            state.loading -> LoadingView(modifier = Modifier.padding(padding))
            state.error != null -> ErrorState(message = state.error!!, modifier = Modifier.padding(padding), onRetry = viewModel::load)
            else -> Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Welcome back, ${state.profile?.fullName?.substringBefore(' ') ?: "there"}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(6.dp))
                        if (state.profile?.isVerified == true) {
                            VerificationBadge(true)
                            Spacer(Modifier.height(6.dp))
                            Text("Your profile is ready for families to discover.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .78f))
                        } else {
                            Text("Complete your portfolio to help us verify your profile.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .78f))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                MetricGrid(
                    metrics = listOf(
                        Metric("Pending requests", state.pendingRequests.size.toString()),
                        Metric("Completed jobs", state.completedCount.toString()),
                        Metric("Rating", "%.1f".format(state.profile?.averageRating ?: 0.0)),
                        Metric("Total earnings", "R${"%.0f".format(state.earnings?.totalEarnings ?: 0.0)}"),
                    ),
                )

                Spacer(Modifier.height(20.dp))
                Text("Quick actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row {
                    SecondaryButton(text = "Bookings", onClick = onManageBookings, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    SecondaryButton(text = "Availability", onClick = onAvailability, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    SecondaryButton(text = "Edit profile", onClick = onEditProfile, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    SecondaryButton(text = "Earnings", onClick = onEarnings, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    SecondaryButton(text = "Portfolio", onClick = onPortfolio, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    SecondaryButton(text = "My reviews", onClick = onReviews, modifier = Modifier.weight(1f))
                }

                state.viewers?.let { viewers ->
                    Spacer(Modifier.height(20.dp))
                    AppCard(Modifier.fillMaxWidth(), onClick = if (viewers.locked) onPlus else null) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Text(stringResource(R.string.plus_viewers_title, viewers.viewsLast30Days), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                if (viewers.locked) {
                                    PlusBadge(stringResource(R.string.plus_name))
                                }
                            }
                            if (viewers.locked) {
                                Text(stringResource(R.string.plus_viewers_locked), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TextActionButton(text = "Upgrade to see profile viewers", onClick = onPlus)
                            } else {
                                viewers.recent.orEmpty().take(5).forEach { v ->
                                    Text("${v.name} · ${com.nannyapp.util.formatDateTime(v.viewedAt)}", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                if (state.pendingRequests.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Text("Booking requests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    state.pendingRequests.forEach { b ->
                        BookingCard(booking = b, onClick = { onBookingClick(b.id) }, modifier = Modifier.padding(bottom = 8.dp))
                    }
                }

                if (state.upcomingBookings.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Text("Upcoming", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    state.upcomingBookings.forEach { b ->
                        BookingCard(booking = b, onClick = { onBookingClick(b.id) }, modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
