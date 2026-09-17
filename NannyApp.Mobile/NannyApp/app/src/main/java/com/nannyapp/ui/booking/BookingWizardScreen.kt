package com.nannyapp.ui.booking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannyapp.ui.components.*

/** 8-step booking wizard per request #12: nanny (preselected) -> date -> time/duration
 * -> children -> address -> notes -> summary -> payment. */
@Composable
fun BookingWizardScreen(
    onBack: () -> Unit,
    onOpenPayment: (String, Int) -> Unit,
    viewModel: BookingWizardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.paymentUrl) {
        val url = state.paymentUrl
        val bookingId = state.createdBooking?.id
        if (url != null && bookingId != null) onOpenPayment(url, bookingId)
    }

    Scaffold(topBar = { AppTopBar(title = "Book a Nanny", onBack = onBack) }) { padding ->
        if (state.loadingNanny) {
            LoadingView(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        Column(Modifier.padding(padding).fillMaxSize()) {
            LinearProgressIndicator(progress = { state.step / state.totalSteps.toFloat() }, modifier = Modifier.fillMaxWidth())
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp)) {
                Text("Step ${state.step} of ${state.totalSteps}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                when (state.step) {
                    1 -> StepNanny(state)
                    2 -> StepDate(state, viewModel)
                    3 -> StepTimeDuration(state, viewModel)
                    4 -> StepChildren(state, viewModel)
                    5 -> StepAddress(state, viewModel)
                    6 -> StepNotes(state, viewModel)
                    7 -> StepSummary(state)
                    8 -> StepPayment(state)
                }
                if (state.error != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                }
            }
            Row(Modifier.padding(20.dp).fillMaxWidth()) {
                if (state.step > 1) {
                    SecondaryButton(text = "Back", onClick = viewModel::previousStep, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                }
                if (state.step < state.totalSteps) {
                    PrimaryButton(text = "Continue", onClick = { if (viewModel.validateStep()) viewModel.nextStep() }, modifier = Modifier.weight(1f))
                } else {
                    PrimaryButton(text = "Confirm & Pay", onClick = viewModel::confirmAndPay, loading = state.submitting, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StepNanny(state: BookingWizardUiState) {
    val nanny = state.wizard.nanny ?: return
    Text("You're booking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(nanny.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            RatingView(nanny.averageRating, nanny.reviewCount)
            Text("$${"%.0f".format(nanny.hourlyRate)}/hr · ${nanny.location ?: ""}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StepDate(state: BookingWizardUiState, viewModel: BookingWizardViewModel) {
    Text("Choose a date and time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        value = state.wizard.dateTimeIso ?: "",
        onValueChange = viewModel::setDateTime,
        label = { Text("Date & time (YYYY-MM-DD HH:MM)") },
        placeholder = { Text("2026-09-20 14:00") },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(4.dp))
    Text("Tip: use your device calendar to confirm the exact date, then type it here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun StepTimeDuration(state: BookingWizardUiState, viewModel: BookingWizardViewModel) {
    Text("How many hours?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))
    Slider(value = state.wizard.durationHours.toFloat(), onValueChange = { viewModel.setDuration(it.toDouble()) }, valueRange = 1f..12f, steps = 21)
    Text("${state.wizard.durationHours} hours", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    Text("Estimated total: $${"%.2f".format(state.wizard.estimatedAmount)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}

@Composable
private fun StepChildren(state: BookingWizardUiState, viewModel: BookingWizardViewModel) {
    Text("Which children need care?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))
    if (state.children.isEmpty()) {
        Text("You haven't added any children yet. Add one from the Children tab first.", style = MaterialTheme.typography.bodyMedium)
    }
    state.children.forEach { child ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = state.wizard.selectedChildIds.contains(child.id), onCheckedChange = { viewModel.toggleChild(child.id) })
            Text("${child.name}${child.age?.let { " ($it)" } ?: ""}")
        }
    }
}

@Composable
private fun StepAddress(state: BookingWizardUiState, viewModel: BookingWizardViewModel) {
    Text("Where will the care happen?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(value = state.wizard.address, onValueChange = viewModel::setAddress, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun StepNotes(state: BookingWizardUiState, viewModel: BookingWizardViewModel) {
    Text("Anything the nanny should know?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(value = state.wizard.notes, onValueChange = viewModel::setNotes, label = { Text("Additional notes (optional)") }, minLines = 4, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun StepSummary(state: BookingWizardUiState) {
    val w = state.wizard
    Text("Review your booking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))
    SummaryRow("Nanny", w.nanny?.fullName ?: "")
    SummaryRow("Date & time", w.dateTimeIso ?: "")
    SummaryRow("Duration", "${w.durationHours} hours")
    SummaryRow("Hourly rate", "$${"%.2f".format(w.nanny?.hourlyRate ?: 0.0)}")
    SummaryRow("Children", "${w.selectedChildIds.size} selected")
    SummaryRow("Address", w.address)
    if (w.notes.isNotBlank()) SummaryRow("Notes", w.notes)
    Spacer(Modifier.height(12.dp))
    HorizontalDivider()
    Spacer(Modifier.height(12.dp))
    SummaryRow("Total amount", "$${"%.2f".format(w.estimatedAmount)}", emphasize = true)
}

@Composable
private fun StepPayment(state: BookingWizardUiState) {
    Text("Payment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))
    Text(
        "We'll create your booking and open a secure Paystack checkout to pay $${"%.2f".format(state.wizard.estimatedAmount)}. " +
            "Your payment is held in escrow until the job is completed and confirmed.",
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun SummaryRow(label: String, value: String, emphasize: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium, fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal)
    }
}
