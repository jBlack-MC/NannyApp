package com.nannyapp.ui.booking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannyapp.domain.model.BookingStatus
import com.nannyapp.domain.model.UserRole
import com.nannyapp.ui.components.*

/**
 * Shared by parent and nanny (request #13/#16). Buttons shown depend on role
 * and current status, mirroring parent/bookings.php and nanny/bookings.php.
 */
@Composable
fun BookingDetailScreen(
    onBack: () -> Unit,
    onCheckIn: (Int) -> Unit,
    onLeaveReview: (Int) -> Unit,
    viewModel: BookingDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showDisputeDialog by remember { mutableStateOf(false) }
    var disputeReason by remember { mutableStateOf("") }

    Scaffold(topBar = { AppTopBar(title = "Booking details", onBack = onBack) }) { padding ->
        when {
            state.loading -> LoadingView(modifier = Modifier.padding(padding))
            state.error != null -> ErrorState(message = state.error!!, modifier = Modifier.padding(padding), onRetry = viewModel::load)
            state.booking != null -> {
                val b = state.booking!!
                Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(b.bookingRef ?: "Booking #${b.id}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        BookingStatusChip(b.status)
                    }
                    Spacer(Modifier.height(16.dp))
                    DetailRow("Nanny", b.nannyName ?: "-")
                    DetailRow("Parent", b.parentName ?: "-")
                    DetailRow("Date & time", b.dateTime)
                    DetailRow("Duration", "${b.durationHours} hours")
                    DetailRow("Address", b.bookingAddress ?: b.location ?: "-")
                    if (!b.notes.isNullOrBlank()) DetailRow("Notes", b.notes)
                    DetailRow("Amount", "$${"%.2f".format(b.amount)}")
                    DetailRow("Payment status", b.paymentStatus.name.lowercase().replaceFirstChar { it.uppercase() })
                    if (b.payoutStatus.name != "NONE") DetailRow("Payout status", b.payoutStatus.name.lowercase().replaceFirstChar { it.uppercase() })

                    if (state.role == UserRole.PARENT && b.checkInCode != null && b.status == BookingStatus.CONFIRMED) {
                        Spacer(Modifier.height(12.dp))
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Check-in PIN", style = MaterialTheme.typography.titleSmall)
                                Text("Share this with your nanny when they arrive:", style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(6.dp))
                                Text(b.checkInCode, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    if (b.disputeReason != null) {
                        Spacer(Modifier.height(12.dp))
                        Text("Dispute reason: ${b.disputeReason}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }

                    if (state.actionError != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(state.actionError!!, color = MaterialTheme.colorScheme.error)
                    }

                    Spacer(Modifier.height(24.dp))
                    ActionButtons(
                        booking = b, role = state.role, inProgress = state.actionInProgress, viewModel = viewModel,
                        onCheckIn = { onCheckIn(b.id) }, onLeaveReview = { onLeaveReview(b.id) },
                        onDisputeRequest = { showDisputeDialog = true },
                    )
                }
            }
        }
    }

    if (showDisputeDialog) {
        AlertDialog(
            onDismissRequest = { showDisputeDialog = false },
            title = { Text("Report an issue") },
            text = {
                OutlinedTextField(value = disputeReason, onValueChange = { disputeReason = it }, label = { Text("What went wrong?") }, minLines = 3)
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dispute(disputeReason); showDisputeDialog = false }) { Text("Submit") }
            },
            dismissButton = { TextButton(onClick = { showDisputeDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ActionButtons(
    booking: com.nannyapp.domain.model.Booking,
    role: UserRole,
    inProgress: Boolean,
    viewModel: BookingDetailViewModel,
    onCheckIn: () -> Unit,
    onLeaveReview: () -> Unit,
    onDisputeRequest: () -> Unit,
) {
    Column {
        when (role) {
            UserRole.PARENT -> {
                if (booking.canCancel) {
                    SecondaryButton(text = "Cancel booking", onClick = viewModel::cancel, enabled = !inProgress, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                }
                if (booking.awaitingParentConfirmation) {
                    PrimaryButton(text = "Confirm service completed", onClick = viewModel::confirmCompletion, loading = inProgress, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    SecondaryButton(text = "Report an issue", onClick = onDisputeRequest, modifier = Modifier.fillMaxWidth())
                }
                if (booking.status == BookingStatus.COMPLETED) {
                    PrimaryButton(text = "Leave a review", onClick = onLeaveReview, modifier = Modifier.fillMaxWidth())
                }
            }
            UserRole.NANNY -> {
                if (booking.status == BookingStatus.PENDING) {
                    Row {
                        SecondaryButton(text = "Reject", onClick = viewModel::reject, enabled = !inProgress, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(12.dp))
                        PrimaryButton(text = "Accept", onClick = viewModel::accept, loading = inProgress, modifier = Modifier.weight(1f))
                    }
                }
                if (booking.status == BookingStatus.CONFIRMED) {
                    PrimaryButton(text = "Check in with PIN", onClick = onCheckIn, modifier = Modifier.fillMaxWidth())
                }
                if (booking.status == BookingStatus.IN_PROGRESS && booking.checkedOutAt == null) {
                    PrimaryButton(text = "Check out", onClick = viewModel::checkOut, loading = inProgress, modifier = Modifier.fillMaxWidth())
                }
            }
            UserRole.ADMIN -> {}
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
