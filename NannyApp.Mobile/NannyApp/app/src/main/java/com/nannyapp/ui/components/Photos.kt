package com.nannyapp.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage

/** Initials of a name, like the website's avatar(): first letter of the first two words. */
fun initialsOf(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.take(1) }.uppercase()

/**
 * A person's photo from the server. Until it loads, or when there is none or it fails, the
 * person's initials show instead, as on the website.
 */
@Composable
fun PersonPhoto(
    name: String,
    url: String?,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    initialsStyle: TextStyle = MaterialTheme.typography.titleMedium,
) {
    Box(
        modifier.clip(shape).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(initialsOf(name), style = initialsStyle, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        if (!url.isNullOrBlank()) {
            AsyncImage(model = url, contentDescription = name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}

/** An image from the server or website, showing a photo bundled in the app until it loads or if it can't. */
@Composable
fun RemoteImage(
    url: String?,
    @DrawableRes fallback: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val bundled = painterResource(fallback)
    if (url.isNullOrBlank()) {
        Image(bundled, contentDescription, modifier, contentScale = contentScale)
    } else {
        AsyncImage(
            model = url, contentDescription = contentDescription, modifier = modifier,
            placeholder = bundled, error = bundled, fallback = bundled, contentScale = contentScale,
        )
    }
}
