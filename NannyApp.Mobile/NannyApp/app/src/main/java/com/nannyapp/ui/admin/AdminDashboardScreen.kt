package com.nannyapp.ui.admin

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

/** Reproduces admin/dashboard.php's KPI cards (request #24). */
@Composable
fun AdminDashboardScreen(
    onUsers: () -> Unit,
    onVerifications: () -> Unit,
    onBookings: () -> Unit,
    onPayments: () -> Unit,
    onReports: () -> Unit,
    onSupport: () -> Unit,
    viewModel: AdminDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { AppTopBar(title = "Admin Dashboard") }) { padding ->
        when {
            state.loading -> LoadingView(modifier = Modifier.padding(padding))
            state.error != null -> ErrorState(message = state.error!!, modifier = Modifier.padding(padding), onRetry = viewModel::load)
            state.stats != null -> {
                val s = state.stats!!
                Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                    Text("Overview", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Total users", s.totalUsers.toString(), modifier = Modifier.weight(1f))
                        StatCard("Total bookings", s.totalBookings.toString(), modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Parents", s.totalParents.toString(), modifier = Modifier.weight(1f))
                        StatCard("Nannies", s.totalNannies.toString(), modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Verified nannies", s.verifiedNannies.toString(), modifier = Modifier.weight(1f))
                        StatCard("Pending verifications", s.pendingVerifications.toString(), modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Pending bookings", s.pendingBookings.toString(), modifier = Modifier.weight(1f))
                        StatCard("Pending documents", s.pendingDocuments.toString(), modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Total revenue", "$${"%.0f".format(s.totalRevenue)}", modifier = Modifier.weight(1f))
                        StatCard("Open tickets", s.openSupportTickets.toString(), modifier = Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(24.dp))
                    Text("Manage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row { SecondaryButton(text = "Users", onClick = onUsers, modifier = Modifier.weight(1f)); Spacer(Modifier.width(8.dp)); SecondaryButton(text = "Verifications", onClick = onVerifications, modifier = Modifier.weight(1f)) }
                    Spacer(Modifier.height(8.dp))
                    Row { SecondaryButton(text = "Bookings", onClick = onBookings, modifier = Modifier.weight(1f)); Spacer(Modifier.width(8.dp)); SecondaryButton(text = "Payments", onClick = onPayments, modifier = Modifier.weight(1f)) }
                    Spacer(Modifier.height(8.dp))
                    Row { SecondaryButton(text = "Reports", onClick = onReports, modifier = Modifier.weight(1f)); Spacer(Modifier.width(8.dp)); SecondaryButton(text = "Support", onClick = onSupport, modifier = Modifier.weight(1f)) }

                    if (s.topLocations.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        Text("Top locations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        s.topLocations.take(5).forEach { (loc, count) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(loc); Text(count.toString(), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
