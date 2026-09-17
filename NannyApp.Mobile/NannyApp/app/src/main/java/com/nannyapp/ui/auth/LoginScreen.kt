package com.nannyapp.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannyapp.domain.model.UserRole
import com.nannyapp.ui.components.PrimaryButton
import com.nannyapp.ui.components.TextActionButton

@Composable
fun LoginScreen(
    onLoggedIn: (UserRole) -> Unit,
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    onVerifyEmail: () -> Unit,
    onBack: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.loggedInRole) {
        state.loggedInRole?.let(onLoggedIn)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScrollFix(),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Welcome back", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Log in to continue to NannyApp", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = state.email, onValueChange = viewModel::onEmailChange,
            label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.password, onValueChange = viewModel::onPasswordChange,
            label = { Text("Password") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(checked = state.rememberMe, onCheckedChange = viewModel::onRememberMeChange)
            Text("Remember me", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextActionButton(text = "Forgot password?", onClick = onForgotPassword)
        }

        if (state.error != null) {
            Spacer(Modifier.height(8.dp))
            Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            if (state.error!!.contains("verify", ignoreCase = true)) {
                TextActionButton(text = "Verify my email", onClick = onVerifyEmail)
            }
        }

        Spacer(Modifier.height(20.dp))
        PrimaryButton(text = "Log in", onClick = viewModel::login, loading = state.loading, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("Don't have an account? ", style = MaterialTheme.typography.bodyMedium)
            TextActionButton(text = "Register", onClick = onRegister)
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextActionButton(text = "Back to Welcome", onClick = onBack)
        }
    }
}

// small helper kept local: allows the column to scroll on small phones without pulling in
// an extra dependency just for this screen.
@Composable
private fun Modifier.verticalScrollFix(): Modifier {
    val scrollState = androidx.compose.foundation.rememberScrollState()
    return this.then(androidx.compose.foundation.verticalScroll(scrollState))
}
