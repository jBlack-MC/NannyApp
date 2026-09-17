package com.nannyapp.ui.nanny

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannyapp.domain.model.AvailabilitySlot
import com.nannyapp.ui.components.*

@Composable
fun NannyAvailabilityScreen(onBack: () -> Unit, viewModel: NannyAvailabilityViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { AppTopBar(title = "Availability", onBack = onBack) }) { padding ->
        when {
            state.loading -> LoadingView(modifier = Modifier.padding(padding))
            state.error != null -> ErrorState(message = state.error!!, modifier = Modifier.padding(padding), onRetry = viewModel::load)
            else -> Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                state.days.forEach { day ->
                    ElevatedCard(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(DAY_NAMES[day.dayOfWeek], fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Switch(checked = day.isAvailable, onCheckedChange = { viewModel.toggleDay(day.dayOfWeek, it) })
                            }
                            if (day.isAvailable) {
                                Spacer(Modifier.height(8.dp))
                                Row {
                                    OutlinedTextField(
                                        value = day.timeStart, onValueChange = { viewModel.setTimeRange(day.dayOfWeek, it, day.timeEnd) },
                                        label = { Text("Start") }, singleLine = true, modifier = Modifier.weight(1f),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    OutlinedTextField(
                                        value = day.timeEnd, onValueChange = { viewModel.setTimeRange(day.dayOfWeek, day.timeStart, it) },
                                        label = { Text("End") }, singleLine = true, modifier = Modifier.weight(1f),
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Row {
                                    listOf(AvailabilitySlot.MORNING, AvailabilitySlot.AFTERNOON, AvailabilitySlot.EVENING).forEach { slot ->
                                        FilterChip(
                                            selected = day.slots.contains(slot),
                                            onClick = { viewModel.toggleSlot(day.dayOfWeek, slot) },
                                            label = { Text(slot.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                            modifier = Modifier.padding(end = 6.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (state.error != null) Text(state.error!!, color = MaterialTheme.colorScheme.error)
                if (state.saved) Text("Availability updated.", color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                PrimaryButton(text = "Save availability", onClick = viewModel::save, loading = state.saving, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
