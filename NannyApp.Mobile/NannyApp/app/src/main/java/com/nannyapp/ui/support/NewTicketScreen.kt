package com.nannyapp.ui.support

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannyapp.domain.model.SupportCategory
import com.nannyapp.ui.components.AppTopBar
import com.nannyapp.ui.components.PrimaryButton

@Composable
fun NewTicketScreen(onBack: () -> Unit, onSubmitted: () -> Unit, viewModel: NewTicketViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.submitted) { if (state.submitted) onSubmitted() }

    Scaffold(topBar = { AppTopBar(title = "New support ticket", onBack = onBack) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(20.dp)) {
            Text("Category", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row {
                SupportCategory.entries.forEach { c ->
                    FilterChip(
                        selected = state.category == c, onClick = { viewModel.update { it.copy(category = c) } },
                        label = { Text(c.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = state.name, onValueChange = { v -> viewModel.update { it.copy(name = v) } }, label = { Text("Your name") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
            OutlinedTextField(value = state.email, onValueChange = { v -> viewModel.update { it.copy(email = v) } }, label = { Text("Your email") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
            OutlinedTextField(value = state.subject, onValueChange = { v -> viewModel.update { it.copy(subject = v) } }, label = { Text("Subject") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
            OutlinedTextField(value = state.message, onValueChange = { v -> viewModel.update { it.copy(message = v) } }, label = { Text("Message") }, minLines = 5, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))

            if (state.error != null) Text(state.error!!, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            PrimaryButton(text = "Submit ticket", onClick = viewModel::submit, loading = state.loading, modifier = Modifier.fillMaxWidth())
        }
    }
}
