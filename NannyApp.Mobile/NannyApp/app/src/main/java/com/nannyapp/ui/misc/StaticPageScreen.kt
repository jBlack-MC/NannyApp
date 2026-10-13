package com.nannyapp.ui.misc

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nannyapp.BuildConfig
import com.nannyapp.domain.model.PageBlock
import com.nannyapp.domain.model.StaticPage
import com.nannyapp.domain.repository.StaticContentRepository
import com.nannyapp.ui.components.AppCard
import com.nannyapp.ui.components.AppTopBar
import com.nannyapp.ui.components.ErrorState
import com.nannyapp.ui.components.LoadingView
import com.nannyapp.ui.components.SecondaryButton
import com.nannyapp.ui.theme.NannyWarning
import com.nannyapp.util.ErrorMessages
import com.nannyapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

private const val UNPUBLISHED_PLACEHOLDER = "This page has not been published yet. Please check back soon."

/**
 * Shows the public information pages (About/Safety/Pricing/FAQ/Community/
 * Resources/Contact/Terms/Privacy). The copy bundled with the app appears
 * immediately; the server is then asked, in the background, whether an admin
 * has published a newer version.
 */
@HiltViewModel
class StaticPageViewModel @Inject constructor(
    private val repository: StaticContentRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<Resource<StaticPage>>(Resource.Loading)
    val state: StateFlow<Resource<StaticPage>> = _state.asStateFlow()
    private var loadJob: Job? = null

    fun load(pageKey: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val bundled = try {
                repository.getBundledPage(pageKey)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            _state.value = bundled?.let { Resource.Success(it) } ?: Resource.Loading

            val published = withTimeoutOrNull(15_000) { repository.getPublishedPage(pageKey) }
            val fresh = (published as? Resource.Success)?.data
                ?.takeIf { it.body.isNotBlank() && it.body != UNPUBLISHED_PLACEHOLDER }
            when {
                fresh != null && fresh != bundled -> _state.value = Resource.Success(fresh)
                // Being offline is not an error while there is a bundled copy on screen.
                bundled == null -> _state.value = published as? Resource.Error ?: Resource.Error(ErrorMessages.TIMEOUT)
            }
        }
    }
}

@Composable
fun StaticPageScreen(pageKey: String, onBack: () -> Unit, viewModel: StaticPageViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(pageKey) { viewModel.load(pageKey) }

    val title = (state as? Resource.Success)?.data?.title ?: pageKey.replaceFirstChar { it.uppercase() }
    Scaffold(topBar = { AppTopBar(title = title, onBack = onBack) }) { padding ->
        Box(Modifier.padding(padding)) {
            when (val s = state) {
                Resource.Loading -> LoadingView()
                is Resource.Error -> ErrorState(message = s.message, onRetry = { viewModel.load(pageKey) })
                is Resource.Success -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (s.data.blocks.isEmpty()) {
                        Text(s.data.title, style = MaterialTheme.typography.headlineMedium)
                        Text(s.data.body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        // Everything above the first card is the website's centred page heading.
                        val firstCard = s.data.blocks.indexOfFirst { it.type == "card" }
                        s.data.blocks.forEachIndexed { index, block ->
                            PageBlockView(block, centered = firstCard == -1 || index < firstCard)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Forms and live figures for this page are on the website.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SecondaryButton(
                        text = "Open this page on the website",
                        onClick = { uriHandler.openUri(BuildConfig.API_BASE_URL.removeSuffix("api/") + "pages/$pageKey.php") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PageBlockView(block: PageBlock, centered: Boolean, inPriceCard: Boolean = false) {
    val align = if (centered) TextAlign.Center else TextAlign.Start
    val fill = Modifier.fillMaxWidth()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    when (block.type) {
        "card" -> {
            val isPrice = block.blocks.any { it.type == "price" }
            // The website outlines its "Most popular" plan in the lighter brand blue.
            val popular = isPrice && block.blocks.any { it.type == "tag" }
            AppCard(
                modifier = fill,
                border = if (popular) BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
                else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = if (isPrice) Alignment.CenterHorizontally else Alignment.Start,
                ) {
                    block.blocks.forEach { PageBlockView(it, centered = false, inPriceCard = isPrice) }
                }
            }
        }
        "eyebrow" -> Text(
            block.text.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.1.em),
            color = MaterialTheme.colorScheme.primary, textAlign = align, modifier = fill,
        )
        "h1" -> Text(block.text, style = MaterialTheme.typography.headlineLarge, textAlign = align, modifier = fill)
        "h2" -> Text(block.text, style = MaterialTheme.typography.headlineSmall, textAlign = align, modifier = fill.padding(top = 6.dp))
        "h3" -> Text(
            block.text, style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, lineHeight = 24.sp),
            textAlign = if (inPriceCard) TextAlign.Center else align, modifier = fill,
        )
        "icon" -> Text(block.text, fontSize = 28.sp, textAlign = align, modifier = fill)
        "quote" -> Text(block.text, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = muted, modifier = fill)
        "price" -> {
            val amount = block.text.substringBefore('/')
            val period = block.text.substringAfter('/', "")
            Text(buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append(amount) }
                if (period.isNotEmpty()) {
                    withStyle(MaterialTheme.typography.titleSmall.toSpanStyle().copy(color = muted)) { append(" /$period") }
                }
            }, style = MaterialTheme.typography.displaySmall)
        }
        "tag" -> when {
            block.text.startsWith("★") -> Text(block.text, color = NannyWarning, style = MaterialTheme.typography.labelLarge)
            inPriceCard -> Text(
                block.text, style = MaterialTheme.typography.labelMedium, color = Color.White,
                modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 4.dp),
            )
            else -> Text(
                block.text.uppercase(), color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.08.em),
            )
        }
        "li", "li_off" -> {
            val off = block.type == "li_off"
            Row(fill, verticalAlignment = Alignment.Top) {
                if (inPriceCard) {
                    Icon(
                        Icons.Filled.Check, contentDescription = if (off) "Not included" else null,
                        tint = if (off) muted.copy(alpha = 0.5f) else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 3.dp).size(18.dp),
                    )
                } else {
                    Text("•", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(10.dp))
                Text(block.text, style = MaterialTheme.typography.bodyLarge, color = if (off) muted.copy(alpha = 0.6f) else muted)
            }
        }
        else -> Text(
            block.text, style = MaterialTheme.typography.bodyLarge, color = muted,
            textAlign = if (inPriceCard) TextAlign.Center else align, modifier = fill,
        )
    }
}
