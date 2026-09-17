package com.nannyapp.ui.messaging

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nannyapp.ui.components.*

@Composable
fun ConversationListScreen(onConversationClick: (Int, String) -> Unit, viewModel: ConversationListViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { AppTopBar(title = "Messages") }) { padding ->
        when {
            state.loading -> LoadingView(modifier = Modifier.padding(padding))
            state.error != null -> ErrorState(message = state.error!!, modifier = Modifier.padding(padding), onRetry = viewModel::load)
            state.conversations.isEmpty() -> EmptyState(title = "No messages yet", message = "Start a conversation from a nanny or booking.", icon = Icons.Filled.ChatBubbleOutline, modifier = Modifier.padding(padding))
            else -> LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                items(state.conversations, key = { it.withUserId }) { conv ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable_(onClick = { onConversationClick(conv.withUserId, conv.withUserName) }).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                            if (!conv.withUserPhotoUrl.isNullOrBlank()) {
                                AsyncImage(model = conv.withUserPhotoUrl, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                            } else {
                                Icon(Icons.Filled.Person, contentDescription = null)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(conv.withUserName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                            Text(conv.lastMessage, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (conv.unreadCount > 0) {
                            Badge { Text(conv.unreadCount.toString()) }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun Modifier.clickable_(onClick: () -> Unit): Modifier = this.then(
    androidx.compose.foundation.clickable(onClick = onClick),
)
