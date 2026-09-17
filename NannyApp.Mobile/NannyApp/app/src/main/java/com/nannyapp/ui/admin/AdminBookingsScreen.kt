package com.nannyapp.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannyapp.domain.model.BookingStatus
import com.nannyapp.ui.components.*

@Composable
fun AdminBookingsScreen(onBack: () -> Unit, onBookingClick: (Int) -> Unit, viewModel: AdminBookingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { AppTopBar(title = "Bookings", onBack = onBack) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = state.query, onValueChange = viewModel::onQueryChange,
                placeholder = { Text("Search by booking ref, parent, or nanny") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true, modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
            LazyRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = state.statusFilter == null, onClick = { viewModel.setFilter(null) }, label = { Text("All") }) }
                items(BookingStatus.entries) { s -> FilterChip(selected = state.statusFilter == s, onClick = { viewModel.setFilter(s) }, label = { Text(s.label) }) }
            }
            Spacer(Modifier.height(8.dp))
            when {
                state.loading -> LoadingView()
                state.error != null -> ErrorState(message = state.error!!, onRetry = viewModel::load)
                state.bookings.isEmpty() -> EmptyState(title = "No bookings found", message = "Try a different filter.")
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.bookings, key = { it.id }) { booking ->
                        BookingCard(booking = booking, onClick = { onBookingClick(booking.id) })
                    }
                }
            }
        }
    }
}
