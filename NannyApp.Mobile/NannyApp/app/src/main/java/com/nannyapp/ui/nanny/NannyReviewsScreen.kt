package com.nannyapp.ui.nanny

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannyapp.ui.components.*

@Composable
fun NannyReviewsScreen(onBack: () -> Unit, viewModel: NannyReviewsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { AppTopBar(title = "Reviews", onBack = onBack) }) { padding ->
        when {
            state.loading -> LoadingView(modifier = Modifier.padding(padding))
            state.error != null -> ErrorState(message = state.error!!, modifier = Modifier.padding(padding), onRetry = viewModel::load)
            else -> Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("%.1f".format(state.profile?.averageRating ?: 0.0), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        RatingView(state.profile?.averageRating ?: 0.0)
                        Text("${state.reviews.size} reviews", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(16.dp))
                (5 downTo 1).forEach { star ->
                    val count = state.distribution[star] ?: 0
                    val fraction = if (state.reviews.isEmpty()) 0f else count.toFloat() / state.reviews.size
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("$star", modifier = Modifier.width(16.dp))
                        Spacer(Modifier.width(6.dp))
                        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)))
                        Spacer(Modifier.width(6.dp))
                        Text("$count", modifier = Modifier.width(24.dp))
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text("Recent reviews", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                state.reviews.forEach { review ->
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(review.reviewerName ?: "Parent", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(8.dp))
                            RatingView(review.rating.toDouble(), compact = true)
                        }
                        if (!review.comment.isNullOrBlank()) Text(review.comment, style = MaterialTheme.typography.bodyMedium)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
