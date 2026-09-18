package com.nannyapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nannyapp.R

/** A compact, local brand mark that is legible in app bars and on small phones. */
@Composable
fun NannyAppBrand(
    modifier: Modifier = Modifier,
    showTagline: Boolean = false,
    light: Boolean = false,
) {
    val contentColor = if (light) Color.White else MaterialTheme.colorScheme.primary
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (light) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Spacer(Modifier.width(9.dp))
        androidx.compose.foundation.layout.Column {
            Text(
                text = "NannyApp",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
            if (showTagline) {
                Text(
                    text = "Care you can count on",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (light) Color.White.copy(alpha = 0.86f) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
