package com.nannyapp.ui.parent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannyapp.ui.components.AppTopBar
import com.nannyapp.ui.components.PrimaryButton

@Composable
fun ChildFormScreen(onSaved: () -> Unit, onBack: () -> Unit, viewModel: ChildFormViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    Scaffold(topBar = { AppTopBar(title = if (state.id == null) "Add child" else "Edit child", onBack = onBack) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
            Field("Name", state.name) { v -> viewModel.update { it.copy(name = v) } }
            Field("Age", state.age) { v -> viewModel.update { it.copy(age = v) } }
            Field("Gender", state.gender) { v -> viewModel.update { it.copy(gender = v) } }
            Field("Allergies", state.allergies) { v -> viewModel.update { it.copy(allergies = v) } }
            Field("Medical conditions", state.medicalConditions) { v -> viewModel.update { it.copy(medicalConditions = v) } }
            Field("Special needs", state.specialNeeds) { v -> viewModel.update { it.copy(specialNeeds = v) } }
            Field("Favourite activities", state.favouriteActivities) { v -> viewModel.update { it.copy(favouriteActivities = v) } }
            Field("Notes for nannies", state.notesForNannies, singleLine = false) { v -> viewModel.update { it.copy(notesForNannies = v) } }

            if (state.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton(text = "Save", onClick = viewModel::save, loading = state.loading, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Field(label: String, value: String, singleLine: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, singleLine = singleLine, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
}
