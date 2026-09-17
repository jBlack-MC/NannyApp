package com.nannyapp.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nannyapp.ui.components.PrimaryButton
import com.nannyapp.ui.components.SecondaryButton
import com.nannyapp.ui.components.TextActionButton

/** Landing screen per request #6 — branding, hero, primary CTAs, and quick links to
 * the informational pages (About/Services/Safety/FAQ/Contact) served from page_content. */
@Composable
fun WelcomeScreen(
    onFindCare: () -> Unit,
    onBecomeNanny: () -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onStaticPage: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AsyncImage(
            model = "https://images.unsplash.com/photo-1526634332515-d56c5fd16991?w=800",
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(260.dp),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        )
        Column(Modifier.padding(24.dp)) {
            Text("NannyApp", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text("Trusted childcare, on your schedule.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(28.dp))
            PrimaryButton(text = "Find Care", onClick = onFindCare, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            SecondaryButton(text = "Become a Nanny", onClick = onBecomeNanny, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text("Already have an account? ", style = MaterialTheme.typography.bodyMedium)
                TextActionButton(text = "Log in", onClick = onLogin)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text("New here? ", style = MaterialTheme.typography.bodyMedium)
                TextActionButton(text = "Register", onClick = onRegister)
            }
            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text("Learn more", style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            FlowLinks(onStaticPage)
        }
    }
}

@Composable
private fun FlowLinks(onStaticPage: (String) -> Unit) {
    val links = listOf(
        "About" to "about", "Services" to "services", "Safety" to "safety",
        "FAQ" to "faq", "Pricing" to "pricing", "Community" to "community",
        "Resources" to "resources", "Contact" to "contact", "Terms" to "terms", "Privacy" to "privacy",
    )
    Column {
        links.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { (label, key) ->
                    TextActionButton(text = label, onClick = { onStaticPage(key) })
                }
            }
        }
    }
}
