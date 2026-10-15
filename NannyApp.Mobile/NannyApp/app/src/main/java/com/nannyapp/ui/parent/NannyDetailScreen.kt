package com.nannyapp.ui.parent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nannyapp.ui.components.*
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.ui.res.stringResource
import com.nannyapp.R
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Lock
import com.nannyapp.ui.components.AppCard
import com.nannyapp.ui.components.TranslatableText

@Composable
fun NannyDetailScreen(
    onBack: () -> Unit,
    onBookNow: (Int) -> Unit,
    onMessage: (Int, String) -> Unit,
    onReport: (Int, String) -> Unit,
    onPlus: () -> Unit,
    viewModel: NannyDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(title = state.nanny?.fullName ?: "Nanny profile", onBack = onBack, actions = {
                state.nanny?.let { nanny ->
                    IconButton(onClick = { onReport(nanny.userId, nanny.fullName) }) {
                        Icon(Icons.Outlined.Flag, contentDescription = stringResource(R.string.report_a_concern))
                    }
                }
            })
        },
        bottomBar = {
            if (state.nanny != null) {
                Surface(shadowElevation = 8.dp) {
                    Row(Modifier.navigationBarsPadding().padding(16.dp).fillMaxWidth()) {
                        SecondaryButton(text = "Message", onClick = { onMessage(viewModel.nannyId, state.nanny?.fullName ?: "") }, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(12.dp))
                        PrimaryButton(text = "Book now", onClick = { onBookNow(viewModel.nannyId) }, modifier = Modifier.weight(1f))
                    }
                }
            }
        },
    ) { padding ->
        when {
            state.loading -> LoadingView(modifier = Modifier.padding(padding))
            state.error != null -> ErrorState(message = state.error!!, modifier = Modifier.padding(padding), onRetry = viewModel::load)
            state.nanny != null -> {
                val nanny = state.nanny!!
                Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
                    AsyncImage(
                        model = nanny.bannerImageUrl ?: nanny.profileImageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentScale = ContentScale.Crop,
                    )
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(nanny.fullName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            VerificationBadge(nanny.isVerified)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = viewModel::toggleSave) {
                                Icon(if (state.isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        RatingView(nanny.averageRating, nanny.reviewCount)
                        Spacer(Modifier.height(4.dp))
                        Text("${nanny.location ?: "Location not set"} · ${nanny.experienceYears} yrs experience", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("R${"%.0f".format(nanny.hourlyRate)}/hr", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                        if (!nanny.bio.isNullOrBlank()) {
                            SectionHeader("About")
                            TranslatableText(nanny.bio)
                        }
                        if (nanny.skills.isNotEmpty()) {
                            SectionHeader("Skills")
                            FlowChips(nanny.skills)
                        }
                        if (nanny.languages.isNotEmpty()) {
                            SectionHeader("Languages")
                            FlowChips(nanny.languages)
                        }
                        if (nanny.specialisations.isNotEmpty()) {
                            SectionHeader("Specialisations")
                            FlowChips(nanny.specialisations)
                        }
                        if (!nanny.qualifications.isNullOrBlank()) {
                            SectionHeader("Qualifications")
                            Text(nanny.qualifications, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (nanny.extendedLocked) {
                            Spacer(Modifier.height(16.dp))
                            AppCard(Modifier.fillMaxWidth(), onClick = onPlus) {
                                Row(Modifier.padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(stringResource(R.string.plus_locked_title), style = MaterialTheme.typography.titleSmall)
                                        Text(stringResource(R.string.plus_locked_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        } else if (!nanny.verifiedDocuments.isNullOrEmpty()) {
                            SectionHeader(stringResource(R.string.plus_verified_documents))
                            nanny.verifiedDocuments!!.forEach { doc ->
                                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(doc.title.ifBlank { doc.type }, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        if (!nanny.availabilityText.isNullOrBlank()) {
                            SectionHeader("Availability")
                            Text(nanny.availabilityText, style = MaterialTheme.typography.bodyMedium)
                        }

                        SectionHeader("Reviews (${state.reviews.size})")
                        if (state.reviews.isEmpty()) {
                            Text("No reviews yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            state.reviews.take(10).forEach { review ->
                                Column(Modifier.padding(vertical = 8.dp)) {
                                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        Text(review.reviewerName ?: "Parent", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                        Spacer(Modifier.width(8.dp))
                                        RatingView(review.rating.toDouble(), compact = true)
                                    }
                                    if (!review.comment.isNullOrBlank()) {
                                        TranslatableText(review.comment, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                        Spacer(Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Spacer(Modifier.height(16.dp))
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun FlowChips(items: List<String>) {
    Row(Modifier.fillMaxWidth()) {
        items.take(6).forEach {
            SkillChip(it)
            Spacer(Modifier.width(6.dp))
        }
    }
}
