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
import com.nannyapp.domain.model.SupportStatus
import com.nannyapp.domain.model.SupportTicket
import com.nannyapp.ui.components.*

@Composable
fun AdminSupportScreen(onBack: () -> Unit, viewModel: AdminSupportViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var editing by remember { mutableStateOf<SupportTicket?>(null) }

    Scaffold(topBar = { AppTopBar(title = "Support", onBack = onBack) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyRow(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = state.statusFilter == null, onClick = { viewModel.setFilter(null) }, label = { Text("All") }) }
                items(SupportStatus.entries) { s -> FilterChip(selected = state.statusFilter == s, onClick = { viewModel.setFilter(s) }, label = { Text(s.name.lowercase().replaceFirstChar { it.uppercase() }) }) }
            }
            when {
                state.loading -> LoadingView()
                state.error != null -> ErrorState(message = state.error!!, onRetry = viewModel::load)
                state.tickets.isEmpty() -> EmptyState(title = "No tickets", message = "Nothing here for this filter.")
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.tickets, key = { it.id }) { ticket ->
                        ElevatedCard(Modifier.fillMaxWidth(), onClick = { editing = ticket }) {
                            Column(Modifier.padding(14.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(ticket.subject, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    SupportStatusChip(ticket.status)
                                }
                                Text("${ticket.name} · ${ticket.email}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(ticket.message, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { ticket -> TicketEditSheet(ticket, onDismiss = { editing = null }, onSave = { status, notes -> viewModel.updateStatus(ticket.id, status, notes); editing = null }) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TicketEditSheet(ticket: SupportTicket, onDismiss: () -> Unit, onSave: (SupportStatus, String?) -> Unit) {
    var status by remember { mutableStateOf(ticket.status) }
    var notes by remember { mutableStateOf(ticket.adminNotes ?: "") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp).fillMaxWidth()) {
            Text(ticket.subject, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(ticket.message, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Row {
                SupportStatus.entries.forEach { s ->
                    FilterChip(selected = status == s, onClick = { status = s }, label = { Text(s.name.lowercase().replaceFirstChar { it.uppercase() }) }, modifier = Modifier.padding(end = 6.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Admin notes") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            PrimaryButton(text = "Save", onClick = { onSave(status, notes.ifBlank { null }) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
        }
    }
}
