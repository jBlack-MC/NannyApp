package com.nannyapp.ui.parent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannyapp.domain.model.BookingStatus
import com.nannyapp.ui.components.*

@Composable
fun ParentDashboardScreen(
    onFindNannies: () -> Unit,
    onNannyClick: (Int) -> Unit,
    onBookingClick: (Int) -> Unit,
    onSaved: () -> Unit,
    onMessages: () -> Unit,
    onNotifications: () -> Unit,
    viewModel: ParentDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = { AppTopBar(title = "NannyApp", unreadNotifications = state.unreadNotifications, onNotificationsClick = onNotifications) },
    ) { padding ->
        when {
            state.loading -> LoadingView(modifier = Modifier.padding(padding))
            state.error != null -> ErrorState(message = state.error!!, modifier = Modifier.padding(padding), onRetry = viewModel::load)
            else -> Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            ) {
                Text("Welcome back, ${state.user?.fullName?.substringBefore(' ') ?: "there"} 👋", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Profile ${state.profileCompletion}% complete", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(progress = { state.profileCompletion / 100f }, modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(20.dp))
                PrimaryButton(text = "Find Care", onClick = onFindNannies, modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(20.dp))
                Text("Your bookings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Pending", (state.bookingStats[BookingStatus.PENDING] ?: 0).toString(), modifier = Modifier.weight(1f))
                    StatCard("Confirmed", (state.bookingStats[BookingStatus.CONFIRMED] ?: 0).toString(), modifier = Modifier.weight(1f))
                    StatCard("Completed", (state.bookingStats[BookingStatus.COMPLETED] ?: 0).toString(), modifier = Modifier.weight(1f))
                }

                if (state.upcomingBooking != null) {
                    Spacer(Modifier.height(20.dp))
                    Text("Upcoming booking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    BookingCard(booking = state.upcomingBooking!!, onClick = { onBookingClick(state.upcomingBooking!!.id) })
                }

                if (state.savedNannies.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Saved nannies", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        TextActionButton(text = "See all", onClick = onSaved)
                    }
                }

                if (state.recentConversations.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Recent messages", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        TextActionButton(text = "See all", onClick = onMessages)
                    }
                    Spacer(Modifier.height(8.dp))
                    state.recentConversations.forEach { conv ->
                        ElevatedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), onClick = onMessages) {
                            Column(Modifier.padding(12.dp)) {
                                Text(conv.withUserName, fontWeight = FontWeight.SemiBold)
                                Text(conv.lastMessage, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                        }
                    }
                }

                if (state.featuredNannies.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Text("Recommended nannies", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.featuredNannies) { nanny ->
                            Box(Modifier.width(220.dp)) {
                                NannyCard(nanny = nanny, isSaved = false, onClick = { onNannyClick(nanny.userId) }, onToggleSave = {})
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
