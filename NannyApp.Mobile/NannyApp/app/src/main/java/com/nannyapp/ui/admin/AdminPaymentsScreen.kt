package com.nannyapp.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannyapp.domain.model.PaymentStatus
import com.nannyapp.ui.components.*

@Composable
fun AdminPaymentsScreen(onBack: () -> Unit, viewModel: AdminPaymentsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var confirmRefundId by remember { mutableStateOf<Int?>(null) }

    Scaffold(topBar = { AppTopBar(title = "Payments", onBack = onBack) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyRow(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = state.statusFilter == null, onClick = { viewModel.setFilter(null) }, label = { Text("All") }) }
                items(PaymentStatus.entries) { s -> FilterChip(selected = state.statusFilter == s, onClick = { viewModel.setFilter(s) }, label = { Text(s.name.lowercase().replaceFirstChar { it.uppercase() }) }) }
            }
            when {
                state.loading -> LoadingView()
                state.error != null -> ErrorState(message = state.error!!, onRetry = viewModel::load)
                state.payments.isEmpty() -> EmptyState(title = "No payments found", message = "Try a different filter.")
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.payments, key = { it.id }) { payment ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(payment.bookingRef ?: "Booking #${payment.bookingId}", fontWeight = FontWeight.SemiBold)
                                    Text("$${"%.2f".format(payment.amount)}", fontWeight = FontWeight.Bold)
                                }
                                Text("${payment.method} · ${payment.status.name.lowercase()} · payout: ${payment.payoutStatus.name.lowercase()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (payment.status == com.nannyapp.domain.model.PaymentStatus.PAID) {
                                    Spacer(Modifier.height(6.dp))
                                    TextActionButton(text = "Refund", onClick = { confirmRefundId = payment.id })
                                }
                            }
                        }
                    }
                }
            }
        }
        if (confirmRefundId != null) {
            AlertDialog(
                onDismissRequest = { confirmRefundId = null },
                title = { Text("Refund payment?") },
                text = { Text("This will refund the parent and mark the payment as refunded. This cannot be undone.") },
                confirmButton = { TextButton(onClick = { viewModel.refund(confirmRefundId!!); confirmRefundId = null }) { Text("Refund") } },
                dismissButton = { TextButton(onClick = { confirmRefundId = null }) { Text("Cancel") } },
            )
        }
    }
}
