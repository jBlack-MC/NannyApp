package com.nannyapp.ui.booking

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannyapp.ui.components.AppTopBar
import com.nannyapp.ui.components.PrimaryButton

@Composable
fun CheckInScreen(onBack: () -> Unit, onCheckedIn: () -> Unit, viewModel: CheckInViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.success) { if (state.success != null) onCheckedIn() }

    Scaffold(topBar = { AppTopBar(title = "Check in", onBack = onBack) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Ask the parent for their check-in PIN", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = state.pin, onValueChange = viewModel::onPinChange,
                label = { Text("Check-in PIN") }, singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(state.error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                Text("${state.attemptsRemaining} attempts remaining", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(20.dp))
            PrimaryButton(text = "Check in", onClick = viewModel::submit, loading = state.loading, modifier = Modifier.fillMaxWidth())
        }
    }
}
