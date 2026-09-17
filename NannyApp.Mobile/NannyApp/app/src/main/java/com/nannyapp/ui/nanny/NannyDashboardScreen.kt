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

@Composable
fun NannyDashboardScreen(
    onBookingClick: (Int) -> Unit,
    onManageBookings: () -> Unit,
    onAvailability: () -> Unit,
    onEditProfile: () -> Unit,
    onEarnings: () -> Unit,
    onNotifications: () -> Unit,
    viewModel: NannyDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { AppTopBar(title = "NannyApp", unreadNotifications = state.unreadNotifications, onNotificationsClick = onNotifications) }) { padding ->
        when {
            state.loading -> LoadingView(modifier = Modifier.padding(padding))
            state.error != null -> ErrorState(message = state.error!!, modifier = Modifier.padding(padding), onRetry = viewModel::load)
            else -> Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                Text("Welcome back, ${state.profile?.fullName?.substringBefore(' ') ?: "there"} 👋", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                VerificationBadge(state.profile?.isVerified == true)
                if (state.profile?.isVerified != true) {
                    Text("Your account is pending verification. Complete your portfolio to speed this up.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Pending requests", state.pendingRequests.size.toString(), modifier = Modifier.weight(1f))
                    StatCard("Completed jobs", state.completedCount.toString(), modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Rating", "%.1f".format(state.profile?.averageRating ?: 0.0), modifier = Modifier.weight(1f))
                    StatCard("Total earnings", "$${"%.0f".format(state.earnings?.totalEarnings ?: 0.0)}", modifier = Modifier.weight(1f))
                }

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
