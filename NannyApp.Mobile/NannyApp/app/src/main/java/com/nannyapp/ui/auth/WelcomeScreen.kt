package com.nannyapp.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nannyapp.ui.components.NannyAppBrand
import com.nannyapp.ui.components.TextActionButton

/** Native counterpart to the public web landing page. */
@Composable
fun WelcomeScreen(
    onFindCare: () -> Unit,
    onBecomeNanny: () -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onStaticPage: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        LandingHero(onFindCare, onBecomeNanny)
        Column(
            modifier = Modifier.offset(y = (-18).dp).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TrustStrip()
            Text("Childcare that feels personal.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text(
                "Discover verified nannies, compare profiles, and manage bookings in one calm, secure space.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("How it works", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            HowItWorks()
            OutlinedButton(
                onClick = onRegister,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = MaterialTheme.shapes.medium,
            ) { Text("Create an account") }
            Text("Explore NannyApp", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            ExploreGrid(onStaticPage)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Already have an account?", style = MaterialTheme.typography.bodyMedium)
                TextActionButton(text = "Log in", onClick = onLogin)
            }
        }
    }
}

@Composable
private fun HowItWorks() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(
            "1" to "Find care that fits your family",
            "2" to "Review verified profiles and availability",
            "3" to "Book, message, and manage care in one place",
        ).forEach { (number, label) ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(number, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun LandingHero(onFindCare: () -> Unit, onBecomeNanny: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)) {
        Column(
            modifier = Modifier.padding(start = 22.dp, top = 20.dp, end = 22.dp, bottom = 42.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            NannyAppBrand(light = true)
            Text("Trusted childcare,\non your schedule.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(
                "Find professional care with confidence. Every profile, review, and booking is designed around your family.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = .88f),
            )
            Button(
                onClick = onFindCare,
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary),
            ) { Text("Find care", fontWeight = FontWeight.Bold) }
            OutlinedButton(
                onClick = onBecomeNanny,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, SolidColor(Color.White.copy(alpha = .7f))),
            ) { Text("Become a nanny") }
        }
    }
}

@Composable
private fun TrustStrip() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    ) {
        Row(Modifier.padding(vertical = 15.dp, horizontal = 10.dp)) {
            TrustItem("Verified", "nannies", Modifier.weight(1f))
            VerticalDivider(modifier = Modifier.height(34.dp))
            TrustItem("Real", "reviews", Modifier.weight(1f))
            VerticalDivider(modifier = Modifier.height(34.dp))
            TrustItem("Clear", "bookings", Modifier.weight(1f))
        }
    }
}

@Composable
private fun TrustItem(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ExploreGrid(onStaticPage: (String) -> Unit) {
    val links = listOf("How it works" to "about", "Safety" to "safety", "Pricing" to "pricing", "Help centre" to "faq")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        links.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, key) ->
                    OutlinedButton(
                        onClick = { onStaticPage(key) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = MaterialTheme.shapes.small,
                    ) { Text(label, textAlign = TextAlign.Center, maxLines = 1) }
                }
            }
        }
    }
}
