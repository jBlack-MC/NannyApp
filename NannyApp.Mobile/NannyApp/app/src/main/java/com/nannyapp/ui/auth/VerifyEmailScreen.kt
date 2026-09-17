package com.nannyapp.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannyapp.ui.components.AppTopBar
import com.nannyapp.ui.components.PrimaryButton
import com.nannyapp.ui.components.TextActionButton

@Composable
fun VerifyEmailScreen(
    initialToken: String? = null,
    onBack: () -> Unit,
    onVerified: () -> Unit,
    onSkip: () -> Unit = onVerified,
    viewModel: VerifyEmailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.setInitialToken(initialToken) }

    Scaffold(topBar = { AppTopBar(title = "Verify your email", onBack = onBack) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Filled.MarkEmailRead, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text(
                "We sent a verification link to your email. Paste the code from that email below, or tap the link on your phone to come straight here.",
                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = state.token, onValueChange = viewModel::onTokenChange,
                label = { Text("Verification code") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )

            if (state.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(state.error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(16.dp))
            PrimaryButton(text = "Verify", onClick = viewModel::verify, loading = state.loading, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(28.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text("Didn't get an email?", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.email, onValueChange = viewModel::onEmailChange,
                label = { Text("Your email") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            if (state.resendSent) {
                Text("A new verification email is on its way.", color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
            }
            TextActionButton(text = if (state.resendLoading) "Sending…" else "Resend verification email", onClick = viewModel::resend)
            Spacer(Modifier.height(12.dp))
            TextActionButton(text = "Skip for now", onClick = onSkip)
        }
    }

    LaunchedEffect(state.verified) { if (state.verified) onVerified() }
}
