package com.nannyapp.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannyapp.domain.model.NannyProfile
import com.nannyapp.ui.components.*

/** Reproduces admin nanny verification queue (request #26): review profile,
 * ID, certificates, references, then approve/reject with notes. */
@Composable
fun AdminVerificationsScreen(onBack: () -> Unit, viewModel: AdminVerificationsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var reviewing by remember { mutableStateOf<NannyProfile?>(null) }

    Scaffold(topBar = { AppTopBar(title = "Nanny Verification", onBack = onBack) }) { padding ->
        when {
            state.loading -> LoadingView(modifier = Modifier.padding(padding))
            state.error != null -> ErrorState(message = state.error!!, modifier = Modifier.padding(padding), onRetry = viewModel::load)
            state.pending.isEmpty() -> EmptyState(title = "All caught up", message = "No nannies are waiting for verification.", modifier = Modifier.padding(padding))
            else -> LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.pending, key = { it.userId }) { nanny ->
                    ElevatedCard(Modifier.fillMaxWidth(), onClick = { reviewing = nanny }) {
                        Column(Modifier.padding(14.dp)) {
                            Text(nanny.fullName, fontWeight = FontWeight.SemiBold)
                            Text("${nanny.location ?: "-"} · ${nanny.experienceYears} yrs experience", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    reviewing?.let { nanny ->
        VerificationReviewSheet(
            nanny = nanny,
            onDismiss = { reviewing = null },
            onApprove = { notes -> viewModel.approve(nanny.userId, notes); reviewing = null },
            onReject = { notes -> viewModel.reject(nanny.userId, notes); reviewing = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VerificationReviewSheet(nanny: NannyProfile, onDismiss: () -> Unit, onApprove: (String?) -> Unit, onReject: (String?) -> Unit) {
    var notes by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp).fillMaxWidth()) {
            Text(nanny.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("${nanny.location ?: "-"} · ${nanny.experienceYears} yrs experience", style = MaterialTheme.typography.bodyMedium)
            if (!nanny.bio.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(nanny.bio, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(12.dp))
            Text("Documents are reviewed in the Portfolio tab of this nanny's profile; approve or reject the overall verification here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Verification notes (optional)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Row {
                SecondaryButton(text = "Reject", onClick = { onReject(notes.ifBlank { null }) }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                PrimaryButton(text = "Approve", onClick = { onApprove(notes.ifBlank { null }) }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
