package com.nannyapp.ui.nanny

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventBusy
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
fun NannyBookingsScreen(onBookingClick: (Int) -> Unit, viewModel: NannyBookingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { AppTopBar(title = "Bookings") }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyRow(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = state.filter == null, onClick = { viewModel.setFilter(null) }, label = { Text("All") }) }
                items(BookingStatus.entries) { status ->
                    FilterChip(selected = state.filter == status, onClick = { viewModel.setFilter(status) }, label = { Text(status.label) })
                }
            }
            when {
                state.loading -> LoadingView()
                state.error != null -> ErrorState(message = state.error!!, onRetry = viewModel::load)
                viewModel.filteredBookings.isEmpty() -> EmptyState(title = "No bookings", message = "Booking requests will appear here.", icon = Icons.Filled.EventBusy)
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(viewModel.filteredBookings, key = { it.id }) { booking ->
                        BookingCard(booking = booking, onClick = { onBookingClick(booking.id) }, counterpartLabel = "Parent")
                    }
                }
            }
        }
    }
}
