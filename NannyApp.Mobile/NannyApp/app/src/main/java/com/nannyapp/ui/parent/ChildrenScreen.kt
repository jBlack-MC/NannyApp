package com.nannyapp.ui.parent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
fun ChildrenScreen(
    onBack: () -> Unit,
    onAddChild: () -> Unit,
    onEditChild: (Int) -> Unit,
    viewModel: ChildrenViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = { AppTopBar(title = "Children", onBack = onBack) },
        floatingActionButton = { FloatingActionButton(onClick = onAddChild) { Icon(Icons.Filled.Add, contentDescription = "Add child") } },
    ) { padding ->
        when {
            state.loading -> LoadingView(modifier = Modifier.padding(padding))
            state.error != null -> ErrorState(message = state.error!!, modifier = Modifier.padding(padding), onRetry = viewModel::load)
            state.children.isEmpty() -> EmptyState(
                title = "No children added yet", message = "Add your child's details so nannies can care for them properly.",
                icon = Icons.Filled.ChildCare, actionLabel = "Add a child", onAction = onAddChild, modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.children, key = { it.id }) { child ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Text(child.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onEditChild(child.id) }) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                                IconButton(onClick = { viewModel.deleteChild(child.id) }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
                            }
                            if (child.age != null) Text("Age: ${child.age}", style = MaterialTheme.typography.bodySmall)
                            if (!child.allergies.isNullOrBlank()) Text("Allergies: ${child.allergies}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            if (!child.specialNeeds.isNullOrBlank()) Text("Special needs: ${child.specialNeeds}", style = MaterialTheme.typography.bodySmall)
                            if (!child.notesForNannies.isNullOrBlank()) Text("Notes: ${child.notesForNannies}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
