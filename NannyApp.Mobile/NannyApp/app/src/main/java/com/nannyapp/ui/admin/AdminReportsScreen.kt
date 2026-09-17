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

/** Request #29: reports for users/nannies/parents/bookings/revenue/payments/reviews/support,
 * built from the same AdminDashboardStats payload used by the KPI dashboard. */
@Composable
fun AdminReportsScreen(onBack: () -> Unit, viewModel: AdminDashboardViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { AppTopBar(title = "Reports", onBack = onBack) }) { padding ->
        when {
            state.loading -> LoadingView(modifier = Modifier.padding(padding))
            state.error != null -> ErrorState(message = state.error!!, modifier = Modifier.padding(padding), onRetry = viewModel::load)
            state.stats != null -> {
                val s = state.stats!!
                Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                    Text("Bookings by status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    s.bookingsByStatus.entries.sortedByDescending { it.value }.forEach { (status, count) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            BookingStatusChip(status)
                            Text(count.toString(), fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Text("Revenue trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    SimpleBarRow(s.revenueByDay.map { it.first to it.second })

                    Spacer(Modifier.height(20.dp))
                    Text("User registrations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    SimpleBarRow(s.registrationsByDay.map { it.first to it.second.toDouble() })

                    Spacer(Modifier.height(20.dp))
                    Text("Top earning nannies", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    s.topEarningNannies.take(10).forEach { (nanny, earnings) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(nanny.fullName)
                            Text("$${"%.0f".format(earnings)}", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Text("Top locations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    s.topLocations.take(10).forEach { (loc, count) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(loc)
                            Text(count.toString(), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

/** Minimal dependency-free bar visualization (avoids pulling in a full charting
 * library just for this summary view; swap for Vico/MPAndroidChart if desired). */
@Composable
private fun SimpleBarRow(data: List<Pair<String, Double>>) {
    val max = data.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0
    Column {
        data.takeLast(14).forEach { (label, value) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(70.dp))
                LinearProgressIndicator(progress = { (value / max).toFloat() }, modifier = Modifier.weight(1f).height(10.dp))
                Spacer(Modifier.width(6.dp))
                Text("%.0f".format(value), style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(50.dp))
            }
        }
    }
}
