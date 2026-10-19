package com.nannyapp.ui.auth

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannyapp.R
import com.nannyapp.domain.model.HomeNanny
import com.nannyapp.domain.model.Testimonial
import com.nannyapp.ui.components.AppCard
import com.nannyapp.ui.components.NannyAppBrand
import com.nannyapp.ui.components.PersonPhoto
import com.nannyapp.ui.components.RatingView
import com.nannyapp.ui.components.RemoteImage
import com.nannyapp.ui.components.PrimaryButton
import com.nannyapp.ui.components.SecondaryButton
import com.nannyapp.ui.components.TextActionButton
import com.nannyapp.ui.theme.NannyGradientEnd
import com.nannyapp.ui.theme.NannyGradientStart
import androidx.compose.ui.platform.LocalDensity

/**
 * Native counterpart of the website's home page (NannyApp.Web/index.php): the same headline,
 * photos and sections. Gallery photos, top-rated nannies and testimonials come from the server;
 * the bundled photos show until they load and whenever the phone is offline.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WelcomeScreen(
    onFindCare: () -> Unit,
    onBecomeNanny: () -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onStaticPage: (String) -> Unit,
    onSettings: () -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel(),
) {
    val home by viewModel.home.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        Hero(onFindCare, onBecomeNanny, onLogin, onSettings)
        Column(
            // Keeps the last links clear of the phone's navigation buttons.
            Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(36.dp),
        ) {
            AboutSection(onLearnMore = { onStaticPage("about") })
            CardSection(
                eyebrow = R.string.welcome_services_eyebrow, title = R.string.welcome_services_title,
                items = listOf(
                    Feature(Icons.Outlined.WbSunny, R.string.welcome_service_full_title, R.string.welcome_service_full_body),
                    Feature(Icons.Outlined.Schedule, R.string.welcome_service_part_title, R.string.welcome_service_part_body),
                    Feature(Icons.Outlined.Celebration, R.string.welcome_service_sitting_title, R.string.welcome_service_sitting_body),
                    Feature(Icons.Outlined.Bedtime, R.string.welcome_service_night_title, R.string.welcome_service_night_body),
                ),
            )
            CardSection(
                eyebrow = R.string.welcome_why_eyebrow, title = R.string.welcome_why_title,
                items = listOf(
                    Feature(Icons.Outlined.VerifiedUser, R.string.welcome_why_checks_title, R.string.welcome_why_checks_body),
                    Feature(Icons.Outlined.StarBorder, R.string.welcome_why_reviews_title, R.string.welcome_why_reviews_body),
                    Feature(Icons.Outlined.Tune, R.string.welcome_why_matching_title, R.string.welcome_why_matching_body),
                    Feature(Icons.Outlined.Assignment, R.string.welcome_why_records_title, R.string.welcome_why_records_body),
                ),
            )
            if (home.topNannies.isNotEmpty()) TopNannies(home.topNannies, onViewAll = onFindCare)
            Gallery(home.galleryImages)
            if (home.testimonials.isNotEmpty()) Testimonials(home.testimonials)
            HowItWorks()
            SafetyBand(onSafety = { onStaticPage("safety") })
            NannyBand(onBecomeNanny)
            ClosingCall(onRegister, onLogin)
            ExploreLinks(onStaticPage)
        }
    }
}

private data class Feature(val icon: ImageVector, @StringRes val title: Int, @StringRes val body: Int)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Hero(onFindCare: () -> Unit, onBecomeNanny: () -> Unit, onLogin: () -> Unit, onSettings: () -> Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))) {
        Image(
            painter = painterResource(R.drawable.welcome_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        // The website's blue overlay keeps white text readable over the photo.
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(listOf(NannyGradientStart.copy(alpha = .78f), Color(0xFF0D1F33).copy(alpha = .92f))),
            ),
        )
        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NannyAppBrand(light = true, modifier = Modifier.weight(1f))
                IconButton(onClick = onSettings) {
                    Icon(Icons.Outlined.Language, contentDescription = stringResource(R.string.settings_title), tint = Color.White)
                }
            }
            Spacer(Modifier.height(36.dp))
            Text(
                stringResource(R.string.welcome_eyebrow).uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.1.em),
                color = Color(0xFF8EC9F8),
            )
            Text(stringResource(R.string.welcome_headline), style = MaterialTheme.typography.displaySmall, color = Color.White)
            Text(stringResource(R.string.welcome_lead), style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = .9f))
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onFindCare,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = NannyGradientStart),
            ) { Text(stringResource(R.string.welcome_find_care), style = MaterialTheme.typography.titleMedium) }
            OutlinedButton(
                onClick = onBecomeNanny,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = .7f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            ) { Text(stringResource(R.string.welcome_become_nanny), style = MaterialTheme.typography.titleMedium) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(R.string.welcome_point_checked, R.string.welcome_point_reviews, R.string.welcome_point_records).forEach {
                    Text(
                        stringResource(it),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.background(Color.White.copy(alpha = .14f), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.welcome_have_account), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = .85f))
                TextButton(onClick = onLogin) { Text(stringResource(R.string.welcome_log_in), color = Color.White, style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

@Composable
private fun Eyebrow(@StringRes text: Int, center: Boolean = false) {
    Text(
        stringResource(text).uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.1.em),
        color = MaterialTheme.colorScheme.primary,
        textAlign = if (center) TextAlign.Center else TextAlign.Start,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun AboutSection(onLearnMore: () -> Unit) {
    AppCard(Modifier.fillMaxWidth()) {
        Image(
            painter = painterResource(R.drawable.welcome_about),
            contentDescription = stringResource(R.string.welcome_about_image),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(190.dp),
        )
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Eyebrow(R.string.welcome_about_eyebrow)
            Text(stringResource(R.string.welcome_about_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.welcome_about_body1), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.welcome_about_body2), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextActionButton(text = stringResource(R.string.welcome_learn_more), onClick = onLearnMore)
        }
    }
}

@Composable
private fun CardSection(@StringRes eyebrow: Int, @StringRes title: Int, items: List<Feature>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Eyebrow(eyebrow, center = true)
        Text(stringResource(title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // Two narrow columns only when each card has room for its text at the current text size.
            val twoColumns = maxWidth >= 360.dp * LocalDensity.current.fontScale
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (twoColumns) {
                    items.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
                            row.forEach { feature ->
                                AppCard(Modifier.weight(1f).fillMaxHeight()) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconTile(feature.icon)
                                        Text(stringResource(feature.title), style = MaterialTheme.typography.titleMedium)
                                        Text(stringResource(feature.body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items.forEach { feature ->
                        AppCard(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                                IconTile(feature.icon)
                                Spacer(Modifier.width(14.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(stringResource(feature.title), style = MaterialTheme.typography.titleMedium)
                                    Text(stringResource(feature.body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconTile(icon: ImageVector) {
    Box(
        Modifier.size(44.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
}

@Composable
private fun HowItWorks() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Eyebrow(R.string.welcome_how_eyebrow, center = true)
        Text(stringResource(R.string.welcome_how_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        listOf(R.string.welcome_step1, R.string.welcome_step2, R.string.welcome_step3).forEachIndexed { index, step ->
            AppCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(36.dp).background(Brush.linearGradient(listOf(NannyGradientStart, NannyGradientEnd)), RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center,
                    ) { Text("${index + 1}", style = MaterialTheme.typography.titleMedium, color = Color.White) }
                    Spacer(Modifier.width(14.dp))
                    Text(stringResource(step), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun SafetyBand(onSafety: () -> Unit) {
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IconTile(Icons.Outlined.Shield)
            Text(stringResource(R.string.welcome_safety_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.welcome_safety_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextActionButton(text = stringResource(R.string.welcome_safety_link), onClick = onSafety)
        }
    }
}

@Composable
private fun NannyBand(onBecomeNanny: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.background(Brush.linearGradient(listOf(NannyGradientStart, NannyGradientEnd))).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.welcome_nanny_eyebrow).uppercase(), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.1.em), color = Color.White.copy(alpha = .85f))
            Text(stringResource(R.string.welcome_nanny_title), style = MaterialTheme.typography.headlineSmall, color = Color.White)
            listOf(R.string.welcome_nanny_point1, R.string.welcome_nanny_point2, R.string.welcome_nanny_point3).forEach {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.padding(top = 2.dp).size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, color = Color.White)
                }
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onBecomeNanny,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = NannyGradientStart),
            ) { Text(stringResource(R.string.welcome_nanny_cta), style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@Composable
private fun ClosingCall(onRegister: () -> Unit, onLogin: () -> Unit) {
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.welcome_cta_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(stringResource(R.string.welcome_cta_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            PrimaryButton(text = stringResource(R.string.welcome_create_account), onClick = onRegister, modifier = Modifier.fillMaxWidth())
            SecondaryButton(text = stringResource(R.string.welcome_log_in), onClick = onLogin, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ExploreLinks(onStaticPage: (String) -> Unit) {
    val links = listOf(
        R.string.page_about to "about", R.string.page_safety to "safety", R.string.page_pricing to "pricing",
        R.string.page_faq to "faq", R.string.page_community to "community", R.string.page_resources to "resources",
        R.string.page_contact to "contact", R.string.page_terms to "terms", R.string.page_privacy to "privacy",
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.welcome_explore), style = MaterialTheme.typography.titleMedium)
        links.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, key) ->
                    TextButton(onClick = { onStaticPage(key) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(label), style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Photos bundled in the app: the website's home-page gallery, shown offline and while loading. */
private val bundledGallery = listOf(
    R.drawable.welcome_hero, R.drawable.welcome_about, R.drawable.welcome_gallery_1,
    R.drawable.welcome_gallery_2, R.drawable.welcome_gallery_3, R.drawable.welcome_gallery_4,
)

@Composable
private fun SectionTitle(@StringRes eyebrow: Int, @StringRes title: Int) {
    Eyebrow(eyebrow, center = true)
    Text(stringResource(title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun TopNannies(nannies: List<HomeNanny>, onViewAll: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(R.string.welcome_nannies_eyebrow, R.string.welcome_nannies_title)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(nannies, key = { it.userId }) { nanny ->
                AppCard(Modifier.width(220.dp), onClick = onViewAll) {
                    PersonPhoto(
                        name = nanny.fullName, url = nanny.photoUrl, shape = RoundedCornerShape(0.dp),
                        initialsStyle = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                    )
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(nanny.fullName, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        nanny.location?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        RatingView(nanny.averageRating, nanny.reviewCount, compact = true)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.welcome_years_experience, nanny.experienceYears), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(R.string.welcome_rate_per_hour, nanny.hourlyRate.toInt()), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        TextActionButton(text = stringResource(R.string.welcome_view_all), onClick = onViewAll)
    }
}

@Composable
private fun Gallery(remote: List<String>) {
    // The server's photos when it answered, else the bundled ones; each remote photo shows a bundled one until it loads.
    val count = if (remote.isNotEmpty()) remote.size else bundledGallery.size
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(R.string.welcome_gallery_eyebrow, R.string.welcome_gallery_title)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(count) { index ->
                RemoteImage(
                    url = remote.getOrNull(index),
                    fallback = bundledGallery[index % bundledGallery.size],
                    contentDescription = stringResource(R.string.welcome_gallery_image),
                    modifier = Modifier.size(width = 220.dp, height = 165.dp).clip(RoundedCornerShape(16.dp)),
                )
            }
        }
    }
}

@Composable
private fun Testimonials(testimonials: List<Testimonial>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(R.string.welcome_testimonials_eyebrow, R.string.welcome_testimonials_title)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(testimonials) { _, t ->
                AppCard(Modifier.width(280.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row {
                            repeat(5) { i ->
                                Icon(
                                    if (i < t.rating) Icons.Filled.Star else Icons.Outlined.StarBorder, contentDescription = null,
                                    tint = Color(0xFFF5A623), modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Text("“${t.comment}”", style = MaterialTheme.typography.bodyMedium, maxLines = 6)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PersonPhoto(name = t.parentName, url = null, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(t.parentName, style = MaterialTheme.typography.titleSmall)
                                Text(stringResource(R.string.welcome_booked_with, t.nannyName), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
