package com.nannyapp.ui.parent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannyapp.domain.model.AvailabilitySlot
import com.nannyapp.domain.model.NannySortOption
import com.nannyapp.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindNanniesScreen(
    onNannyClick: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: FindNanniesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = { AppTopBar(title = "Find Nannies", onBack = onBack) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.filters.query, onValueChange = viewModel::onQueryChange,
                    placeholder = { Text("Search by name or location") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true, modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { viewModel.toggleFilterSheet(true) }) { Icon(Icons.Filled.FilterList, contentDescription = "Filters") }
            }

            when {
                state.loading -> LoadingView()
                state.error != null -> ErrorState(message = state.error!!, onRetry = viewModel::search)
                state.nannies.isEmpty() -> EmptyState(title = "No nannies found", message = "Try adjusting your search or filters.")
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.nannies, key = { it.userId }) { nanny ->
                        NannyCard(
                            nanny = nanny,
                            isSaved = state.savedIds.contains(nanny.userId),
                            onClick = { onNannyClick(nanny.userId) },
                            onToggleSave = { viewModel.toggleSave(nanny.userId) },
                        )
                    }
                }
            }
        }

        if (state.showFilterSheet) {
            ModalBottomSheet(onDismissRequest = { viewModel.toggleFilterSheet(false) }) {
                FilterSheetContent(viewModel)
            }
        }
    }
}

@Composable
private fun FilterSheetContent(viewModel: FindNanniesViewModel) {
    val state by viewModel.state.collectAsState()
    val f = state.filters
    Column(Modifier.padding(20.dp).fillMaxWidth()) {
        Text("Filters", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = f.location ?: "", onValueChange = { v -> viewModel.updateFilters { it.copy(location = v.ifBlank { null }) } }, label = { Text("Location") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Row {
            OutlinedTextField(
                value = f.minRate?.toString() ?: "", onValueChange = { v -> viewModel.updateFilters { it.copy(minRate = v.toDoubleOrNull()) } },
                label = { Text("Min $/hr") }, singleLine = true, modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = f.maxRate?.toString() ?: "", onValueChange = { v -> viewModel.updateFilters { it.copy(maxRate = v.toDoubleOrNull()) } },
                label = { Text("Max $/hr") }, singleLine = true, modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = f.minExperience?.toString() ?: "", onValueChange = { v -> viewModel.updateFilters { it.copy(minExperience = v.toIntOrNull()) } },
            label = { Text("Min years experience") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(checked = f.verifiedOnly, onCheckedChange = { v -> viewModel.updateFilters { it.copy(verifiedOnly = v) } })
            Text("Verified nannies only")
        }
        Spacer(Modifier.height(8.dp))
        Text("Sort by", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NannySortOption.entries.forEach { option ->
                FilterChip(
                    selected = f.sortBy == option,
                    onClick = { viewModel.updateFilters { it.copy(sortBy = option) } },
                    label = { Text(sortLabel(option)) },
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(text = "Apply filters", onClick = viewModel::applyFilters, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
    }
}

private fun sortLabel(o: NannySortOption) = when (o) {
    NannySortOption.RATING -> "Rating"
    NannySortOption.EXPERIENCE -> "Experience"
    NannySortOption.PRICE_LOW -> "Price: low"
    NannySortOption.PRICE_HIGH -> "Price: high"
}
