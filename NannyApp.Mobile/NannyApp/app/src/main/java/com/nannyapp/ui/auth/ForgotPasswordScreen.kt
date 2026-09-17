package com.nannyapp.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannyapp.ui.components.AppTopBar
import com.nannyapp.ui.components.PrimaryButton

@Composable
fun ForgotPasswordScreen(onBack: () -> Unit, viewModel: ForgotPasswordViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { AppTopBar(title = "Reset password", onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            if (state.sent) {
                Text("Check your email", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("If an account exists for that email, we've sent password reset instructions.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("Enter your email and we'll send you a link to reset your password.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = state.email, onValueChange = viewModel::onEmailChange, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (state.error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(16.dp))
                PrimaryButton(text = "Send reset link", onClick = viewModel::submit, loading = state.loading, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
