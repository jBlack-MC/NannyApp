package com.nannyapp.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannyapp.domain.model.UserRole
import com.nannyapp.ui.components.AppTopBar
import com.nannyapp.ui.components.PrimaryButton

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onBack: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.registered) {
        if (state.registered) onRegistered()
    }

    Scaffold(topBar = { AppTopBar(title = "Create account", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))
            Text("I am a…", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                RoleOption("Parent", state.role == UserRole.PARENT, Modifier.weight(1f)) { viewModel.update { it.copy(role = UserRole.PARENT) } }
                Spacer(Modifier.width(12.dp))
                RoleOption("Nanny", state.role == UserRole.NANNY, Modifier.weight(1f)) { viewModel.update { it.copy(role = UserRole.NANNY) } }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Basic information")
            LabeledField("Full name", state.fullName) { viewModel.update { s -> s.copy(fullName = it) } }
            LabeledField("Email", state.email) { viewModel.update { s -> s.copy(email = it) } }
            LabeledField("Phone", state.phone) { viewModel.update { s -> s.copy(phone = it) } }
            LabeledField("Date of birth (YYYY-MM-DD)", state.dateOfBirth) { viewModel.update { s -> s.copy(dateOfBirth = it) } }
            LabeledField("Address", state.address) { viewModel.update { s -> s.copy(address = it) } }
            GenderDropdown(state.gender) { viewModel.update { s -> s.copy(gender = it) } }
            LabeledField("Password", state.password, isPassword = true) { viewModel.update { s -> s.copy(password = it) } }
            LabeledField("Confirm password", state.confirmPassword, isPassword = true) { viewModel.update { s -> s.copy(confirmPassword = it) } }

            if (state.role == UserRole.PARENT) {
                Spacer(Modifier.height(20.dp))
                SectionLabel("Parent details")
                LabeledField("Number of children", state.numberOfChildren) { viewModel.update { s -> s.copy(numberOfChildren = it) } }
                LabeledField("Emergency contact name", state.emergencyContactName) { viewModel.update { s -> s.copy(emergencyContactName = it) } }
                LabeledField("Emergency contact phone", state.emergencyContact) { viewModel.update { s -> s.copy(emergencyContact = it) } }
                LabeledField("Emergency contact relationship", state.emergencyContactRelationship) { viewModel.update { s -> s.copy(emergencyContactRelationship = it) } }
            } else {
                Spacer(Modifier.height(20.dp))
                SectionLabel("Nanny profile")
                LabeledField("Bio", state.bio, singleLine = false) { viewModel.update { s -> s.copy(bio = it) } }
                LabeledField("Years of experience", state.experienceYears) { viewModel.update { s -> s.copy(experienceYears = it) } }
                LabeledField("Hourly rate (USD)", state.hourlyRate) { viewModel.update { s -> s.copy(hourlyRate = it) } }
                LabeledField("Location", state.location) { viewModel.update { s -> s.copy(location = it) } }
                LabeledField("Skills (comma separated)", state.skills) { viewModel.update { s -> s.copy(skills = it) } }
                LabeledField("Languages (comma separated)", state.languages) { viewModel.update { s -> s.copy(languages = it) } }
                LabeledField("Qualifications", state.qualifications) { viewModel.update { s -> s.copy(qualifications = it) } }
                LabeledField("Specialisations (comma separated)", state.specialisations) { viewModel.update { s -> s.copy(specialisations = it) } }
            }

            if (state.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(20.dp))
            PrimaryButton(text = "Create account", onClick = viewModel::register, loading = state.loading, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RoleOption(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = if (selected) CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else CardDefaults.elevatedCardColors()
    ElevatedCard(onClick = onClick, modifier = modifier, colors = colors) {
        Box(Modifier.padding(vertical = 16.dp).fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun LabeledField(label: String, value: String, isPassword: Boolean = false, singleLine: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        singleLine = singleLine,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenderDropdown(selected: com.nannyapp.domain.model.Gender, onSelect: (com.nannyapp.domain.model.Gender) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = com.nannyapp.domain.model.Gender.entries
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = Modifier.padding(bottom = 12.dp)) {
        OutlinedTextField(
            value = selected.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() },
            onValueChange = {}, readOnly = true, label = { Text("Gender") },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }) },
                    onClick = { onSelect(option); expanded = false },
                )
            }
        }
    }
}
