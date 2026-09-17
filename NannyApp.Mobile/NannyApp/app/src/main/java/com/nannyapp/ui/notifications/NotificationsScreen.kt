package com.nannyapp.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannyapp.ui.components.*

@Composable
fun NotificationsScreen(onBack: () -> Unit, viewModel: NotificationsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(title = "Notifications", onBack = onBack, actions = {
                TextButton(onClick = viewModel::markAllRead) { Text("Mark all read") }
            })
        },
    ) { padding ->
        when {
            state.loading -> LoadingView(modifier = Modifier.padding(padding))
            state.notifications.isEmpty() -> EmptyState(title = "No notifications", message = "You're all caught up.", icon = Icons.Filled.NotificationsNone, modifier = Modifier.padding(padding))
            else -> LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                items(state.notifications, key = { it.id }) { n ->
                    Row(
                        Modifier.fillMaxWidth()
                            .background(if (!n.isRead) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface)
                            .clickable_(onClick = { viewModel.markRead(n.id) })
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        if (!n.isRead) { UnreadDot(modifier = Modifier.padding(top = 6.dp, end = 8.dp)) } else Spacer(Modifier.width(16.dp))
                        Column {
                            Text(n.title, fontWeight = FontWeight.SemiBold)
                            Text(n.message, style = MaterialTheme.typography.bodyMedium)
                            Text(n.createdAt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun Modifier.clickable_(onClick: () -> Unit): Modifier = this.then(androidx.compose.foundation.clickable(onClick = onClick))
