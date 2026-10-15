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
import com.nannyapp.ui.components.TextActionButton
import com.nannyapp.domain.model.NannySearchFilters
import com.nannyapp.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll

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
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp).fillMaxWidth()) {
        Text("Filters", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = f.location ?: "", onValueChange = { v -> viewModel.updateFilters { it.copy(location = v.ifBlank { null }) } }, label = { Text("Location") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Row {
            OutlinedTextField(
                value = f.minRate?.toString() ?: "", onValueChange = { v -> viewModel.updateFilters { it.copy(minRate = v.toDoubleOrNull()) } },
                label = { Text("Min R/hr") }, singleLine = true, modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = f.maxRate?.toString() ?: "", onValueChange = { v -> viewModel.updateFilters { it.copy(maxRate = v.toDoubleOrNull()) } },
                label = { Text("Max R/hr") }, singleLine = true, modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = f.language ?: "", onValueChange = { v -> viewModel.updateFilters { it.copy(language = v.ifBlank { null }) } },
            label = { Text(stringResource(R.string.filter_language)) }, placeholder = { Text(stringResource(R.string.filter_language_hint)) },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        FilterChipRow(
            title = stringResource(R.string.filter_experience),
            options = listOf(null, 1, 3, 5, 10),
            selected = f.minExperience,
            label = { if (it == null) stringResource(R.string.filter_any) else stringResource(R.string.filter_years_plus, it) },
            onSelect = { v -> viewModel.updateFilters { it.copy(minExperience = v) } },
        )
        FilterChipRow(
            title = stringResource(R.string.filter_rating),
            options = listOf(null, 3.0, 4.0, 4.5),
            selected = f.minRating,
            label = { if (it == null) stringResource(R.string.filter_any) else stringResource(R.string.filter_stars_plus, it.toString().removeSuffix(".0")) },
            onSelect = { v -> viewModel.updateFilters { it.copy(minRating = v) } },
        )
        FilterChipRow(
            title = stringResource(R.string.filter_availability),
            options = listOf(null) + AvailabilitySlot.entries,
            selected = f.availableSlot,
            label = {
                when (it) {
                    null -> stringResource(R.string.filter_any)
                    AvailabilitySlot.MORNING -> stringResource(R.string.filter_morning)
                    AvailabilitySlot.AFTERNOON -> stringResource(R.string.filter_afternoon)
                    AvailabilitySlot.EVENING -> stringResource(R.string.filter_evening)
                }
            },
            onSelect = { v -> viewModel.updateFilters { it.copy(availableSlot = v) } },
        )
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(checked = f.verifiedOnly, onCheckedChange = { v -> viewModel.updateFilters { it.copy(verifiedOnly = v) } })
            Text("Verified nannies only")
        }
        Spacer(Modifier.height(8.dp))
        Text("Sort by", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        Spacer(Modifier.height(8.dp))
        TextActionButton(
            text = stringResource(R.string.filter_clear),
            onClick = { viewModel.updateFilters { NannySearchFilters(query = it.query) } },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun <T> FilterChipRow(title: String, options: List<T?>, selected: T?, label: @Composable (T?) -> String, onSelect: (T?) -> Unit) {
    Text(title, style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(6.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(selected = selected == option, onClick = { onSelect(option) }, label = { Text(label(option)) })
        }
    }
    Spacer(Modifier.height(12.dp))
}

private fun sortLabel(o: NannySortOption) = when (o) {
    NannySortOption.RATING -> "Rating"
    NannySortOption.EXPERIENCE -> "Experience"
    NannySortOption.PRICE_LOW -> "Price: low"
    NannySortOption.PRICE_HIGH -> "Price: high"
}
