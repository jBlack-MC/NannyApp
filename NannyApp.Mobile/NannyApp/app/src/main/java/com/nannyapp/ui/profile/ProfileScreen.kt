package com.nannyapp.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nannyapp.ui.components.*

/** Reproduces account.php: editable identity fields, password change, and
 * profile-completion percentage (request #31). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onLoggedOut: () -> Unit, viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showPasswordSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state.loggedOut) { if (state.loggedOut) onLoggedOut() }

    Scaffold(topBar = { AppTopBar(title = "Profile") }) { padding ->
        when {
            state.loading -> LoadingView(modifier = Modifier.padding(padding))
            state.error != null && state.user == null -> ErrorState(message = state.error!!, modifier = Modifier.padding(padding), onRetry = viewModel::load)
            state.user != null -> Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val pickAvatar = rememberFilePickerLauncher { bytes, fileName -> viewModel.uploadAvatar(bytes, fileName) }
                    Box {
                        Box(Modifier.size(72.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                            if (!state.user!!.profileImageUrl.isNullOrBlank()) {
                                AsyncImage(model = state.user!!.profileImageUrl, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                            } else {
                                Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.fillMaxSize())
                            }
                        }
                        IconButton(
                            onClick = pickAvatar,
                            modifier = Modifier.align(Alignment.BottomEnd).size(28.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                        ) {
                            Icon(Icons.Filled.PhotoCamera, contentDescription = "Change photo", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(state.user!!.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(state.user!!.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Profile ${state.completion}% complete", style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(progress = { state.completion / 100f }, modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(20.dp))
                if (state.editing) {
                    OutlinedTextField(value = state.fullName, onValueChange = { v -> viewModel.update { it.copy(fullName = v) } }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp))
                    OutlinedTextField(value = state.phone, onValueChange = { v -> viewModel.update { it.copy(phone = v) } }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp))
                    OutlinedTextField(value = state.dateOfBirth, onValueChange = { v -> viewModel.update { it.copy(dateOfBirth = v) } }, label = { Text("Date of birth") }, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp))
                    OutlinedTextField(value = state.address, onValueChange = { v -> viewModel.update { it.copy(address = v) } }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp))
                    Row {
                        SecondaryButton(text = "Cancel", onClick = { viewModel.toggleEdit(false) }, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(12.dp))
                        PrimaryButton(text = "Save", onClick = viewModel::save, loading = state.saving, modifier = Modifier.weight(1f))
                    }
                } else {
                    InfoRow("Phone", state.user!!.phone ?: "-")
                    InfoRow("Date of birth", state.user!!.dateOfBirth ?: "-")
                    InfoRow("Address", state.user!!.address ?: "-")
                    Spacer(Modifier.height(12.dp))
                    SecondaryButton(text = "Edit profile", onClick = { viewModel.toggleEdit(true) }, modifier = Modifier.fillMaxWidth())
                }

                Spacer(Modifier.height(12.dp))
                SecondaryButton(text = "Change password", onClick = { showPasswordSheet = true }, modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = { showLogoutConfirm = true }) {
                    Icon(Icons.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(6.dp))
                    Text("Log out", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Log out?") },
            text = { Text("You'll need to log in again to access your account.") },
            confirmButton = { TextButton(onClick = { viewModel.logout(); showLogoutConfirm = false }) { Text("Log out") } },
            dismissButton = { TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") } },
        )
    }

    if (showPasswordSheet) {
        ModalBottomSheet(onDismissRequest = { showPasswordSheet = false }) {
            PasswordChangeContent(viewModel)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PasswordChangeContent(viewModel: ProfileViewModel) {
    val state by viewModel.state.collectAsState()
    Column(Modifier.padding(20.dp).fillMaxWidth()) {
        Text("Change password", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = state.currentPassword, onValueChange = { v -> viewModel.update { it.copy(currentPassword = v) } }, label = { Text("Current password") }, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp))
        OutlinedTextField(value = state.newPassword, onValueChange = { v -> viewModel.update { it.copy(newPassword = v) } }, label = { Text("New password") }, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp))
        OutlinedTextField(value = state.confirmNewPassword, onValueChange = { v -> viewModel.update { it.copy(confirmNewPassword = v) } }, label = { Text("Confirm new password") }, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp))
        if (state.passwordError != null) Text(state.passwordError!!, color = MaterialTheme.colorScheme.error)
        if (state.passwordChanged) Text("Password changed successfully.", color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        PrimaryButton(text = "Update password", onClick = viewModel::changePassword, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
    }
}
