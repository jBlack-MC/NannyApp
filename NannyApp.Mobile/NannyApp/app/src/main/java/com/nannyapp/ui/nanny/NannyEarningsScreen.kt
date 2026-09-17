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

/** Escrow-aware earnings screen (request #17): held vs released, mirroring
 * payments.payout_status in the existing schema. */
@Composable
fun NannyEarningsScreen(onBack: () -> Unit, viewModel: NannyEarningsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { AppTopBar(title = "Earnings", onBack = onBack) }) { padding ->
        when {
            state.loading -> LoadingView(modifier = Modifier.padding(padding))
            state.error != null -> ErrorState(message = state.error!!, modifier = Modifier.padding(padding), onRetry = viewModel::load)
            state.summary != null -> {
                val s = state.summary!!
                Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                    Text("$${"%.2f".format(s.totalEarnings)}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Total earnings", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Held (in escrow)", "$${"%.2f".format(s.heldEarnings)}", modifier = Modifier.weight(1f))
                        StatCard("Released", "$${"%.2f".format(s.releasedEarnings)}", modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    StatCard("Completed jobs", s.completedJobs.toString())
                    Text(
                        "Held funds are released once the parent confirms the job as complete, " +
                            "or automatically after 48 hours if there's no dispute.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )

                    Spacer(Modifier.height(20.dp))
                    Text("Recent payments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    s.recentPayments.forEach { p ->
                        ElevatedCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(p.bookingRef ?: "Booking #${p.bookingId}", fontWeight = FontWeight.SemiBold)
                                    Text("$${"%.2f".format(p.amount)}", fontWeight = FontWeight.Bold)
                                }
                                Text(p.payoutStatus.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
