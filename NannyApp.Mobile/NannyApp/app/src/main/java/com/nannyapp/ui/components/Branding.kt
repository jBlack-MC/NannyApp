package com.nannyapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/** A compact, local brand mark that is legible in app bars and on small phones. */
@Composable
fun NannyAppBrand(
    modifier: Modifier = Modifier,
    showTagline: Boolean = false,
    light: Boolean = false,
) {
    val contentColor = if (light) Color.White else MaterialTheme.colorScheme.primary
    if (showTagline && !light) {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(
                model = "file:///android_asset/Icon_Logo.png",
                contentDescription = null,
                modifier = Modifier.size(196.dp),
                contentScale = ContentScale.Fit,
            )
        }
    } else {
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (light) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = "file:///android_asset/Icon_Logo.png",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "Nanny-App",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor,
                )
                if (showTagline) {
                    Text(
                        text = "Care you can count on",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (light) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
