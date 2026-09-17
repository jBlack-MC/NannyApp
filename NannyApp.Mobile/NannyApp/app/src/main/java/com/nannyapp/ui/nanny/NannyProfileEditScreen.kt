package com.nannyapp.ui.nanny

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannyapp.ui.components.AppTopBar
import com.nannyapp.ui.components.ErrorState
import com.nannyapp.ui.components.LoadingView
import com.nannyapp.ui.components.PrimaryButton

@Composable
fun NannyProfileEditScreen(onBack: () -> Unit, viewModel: NannyProfileEditViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { AppTopBar(title = "Edit profile", onBack = onBack) }) { padding ->
        when {
            state.loading -> LoadingView(modifier = Modifier.padding(padding))
            state.error != null && state.bio.isBlank() -> ErrorState(message = state.error!!, modifier = Modifier.padding(padding), onRetry = viewModel::load)
            else -> Column(Modifier.padding(padding).fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
                Field("Bio", state.bio, singleLine = false) { v -> viewModel.update { it.copy(bio = v) } }
                Field("Years of experience", state.experienceYears) { v -> viewModel.update { it.copy(experienceYears = v) } }
                Field("Hourly rate (USD)", state.hourlyRate) { v -> viewModel.update { it.copy(hourlyRate = v) } }
                Field("Location", state.location) { v -> viewModel.update { it.copy(location = v) } }
                Field("Skills (comma separated)", state.skills) { v -> viewModel.update { it.copy(skills = v) } }
                Field("Languages (comma separated)", state.languages) { v -> viewModel.update { it.copy(languages = v) } }
                Field("Qualifications", state.qualifications, singleLine = false) { v -> viewModel.update { it.copy(qualifications = v) } }
                Field("Specialisations (comma separated)", state.specialisations) { v -> viewModel.update { it.copy(specialisations = v) } }
                Field("Availability summary", state.availabilityText, singleLine = false) { v -> viewModel.update { it.copy(availabilityText = v) } }

                if (state.error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                }
                if (state.saved) {
                    Spacer(Modifier.height(8.dp))
                    Text("Profile updated.", color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(16.dp))
                PrimaryButton(text = "Save changes", onClick = viewModel::save, loading = state.saving, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String, singleLine: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, singleLine = singleLine, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
}
