package com.nannyapp.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannyapp.domain.model.AccountStatus
import com.nannyapp.domain.model.UserRole
import com.nannyapp.ui.components.*

@Composable
fun AdminUsersScreen(onBack: () -> Unit, viewModel: AdminUsersViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { AppTopBar(title = "Users", onBack = onBack) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = state.query, onValueChange = viewModel::onQueryChange,
                placeholder = { Text("Search users") }, leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true, modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
            LazyRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = state.roleFilter == null, onClick = { viewModel.setRoleFilter(null) }, label = { Text("All") }) }
                items(UserRole.entries) { role ->
                    FilterChip(selected = state.roleFilter == role, onClick = { viewModel.setRoleFilter(role) }, label = { Text(role.name.lowercase().replaceFirstChar { it.uppercase() }) })
                }
            }
            Spacer(Modifier.height(8.dp))
            when {
                state.loading -> LoadingView()
                state.error != null -> ErrorState(message = state.error!!, onRetry = viewModel::load)
                state.users.isEmpty() -> EmptyState(title = "No users found", message = "Try a different search or filter.")
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.users, key = { it.id }) { user ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(user.fullName, fontWeight = FontWeight.SemiBold)
                                    Text(user.role.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                if (user.status == AccountStatus.SUSPENDED) {
                                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        Text("Suspended", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                                        TextActionButton(text = "Activate", onClick = { viewModel.requestActivate(user.id) })
                                    }
                                } else {
                                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        Text("Active", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                                        TextActionButton(text = "Suspend", onClick = { viewModel.requestSuspend(user.id) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.confirmingUserId != null) {
            AlertDialog(
                onDismissRequest = viewModel::dismissConfirm,
                title = { Text(if (state.confirmingSuspend) "Suspend user?" else "Activate user?") },
                text = { Text(if (state.confirmingSuspend) "They won't be able to log in until reactivated." else "They'll regain access to their account.") },
                confirmButton = { TextButton(onClick = viewModel::confirmAction) { Text("Confirm") } },
                dismissButton = { TextButton(onClick = viewModel::dismissConfirm) { Text("Cancel") } },
            )
        }
    }
}
