package com.nannyapp.ui.support

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SupportAgent
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
fun SupportScreen(onNewTicket: () -> Unit, viewModel: SupportViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = { AppTopBar(title = "Support") },
        floatingActionButton = { FloatingActionButton(onClick = onNewTicket) { Icon(Icons.Filled.Add, contentDescription = "New ticket") } },
    ) { padding ->
        when {
            state.loading -> LoadingView(modifier = Modifier.padding(padding))
            state.error != null -> ErrorState(message = state.error!!, modifier = Modifier.padding(padding), onRetry = viewModel::load)
            state.tickets.isEmpty() -> EmptyState(
                title = "No support tickets", message = "Need help? Submit a ticket and our team will get back to you.",
                icon = Icons.Filled.SupportAgent, actionLabel = "New ticket", onAction = onNewTicket, modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.tickets, key = { it.id }) { ticket ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(ticket.subject, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                SupportStatusChip(ticket.status)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(ticket.message, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                            Spacer(Modifier.height(4.dp))
                            Text(ticket.category.name.lowercase().replaceFirstChar { it.uppercase() } + " · " + ticket.createdAt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (!ticket.adminNotes.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text("Support: ${ticket.adminNotes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
