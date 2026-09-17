package com.nannyapp.ui.booking

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannyapp.ui.components.AppTopBar
import com.nannyapp.ui.components.PrimaryButton

@Composable
fun LeaveReviewScreen(onBack: () -> Unit, onSubmitted: () -> Unit, viewModel: LeaveReviewViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.submitted) { if (state.submitted) onSubmitted() }

    Scaffold(topBar = { AppTopBar(title = "Leave a review", onBack = onBack) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(24.dp)) {
            Text("How was your experience with ${state.nannyName}?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            Row {
                (1..5).forEach { i ->
                    IconButton(onClick = { viewModel.setRating(i) }) {
                        Icon(
                            if (i <= state.rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = "$i stars",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.comment, onValueChange = viewModel::setComment,
                label = { Text("Write your review (optional)") }, minLines = 4, modifier = Modifier.fillMaxWidth(),
            )
            if (state.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(20.dp))
            PrimaryButton(text = "Submit review", onClick = viewModel::submit, loading = state.loading, modifier = Modifier.fillMaxWidth())
        }
    }
}
