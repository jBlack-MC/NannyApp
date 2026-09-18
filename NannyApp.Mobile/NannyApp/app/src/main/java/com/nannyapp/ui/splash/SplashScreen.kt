package com.nannyapp.ui.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannyapp.domain.model.UserRole
import com.nannyapp.ui.auth.SessionCheck
import com.nannyapp.ui.auth.SplashViewModel
import com.nannyapp.ui.components.NannyAppBrand
import coil.compose.AsyncImage

/** Splash screen per request #38: logo + tagline, then routes based on session state. */
@Composable
fun SplashScreen(
    onNavigateToWelcome: () -> Unit,
    onNavigateToParentHome: () -> Unit,
    onNavigateToNannyHome: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        when (val s = state) {
            is SessionCheck.LoggedIn -> when (s.role) {
                UserRole.PARENT -> onNavigateToParentHome()
                UserRole.NANNY -> onNavigateToNannyHome()
                UserRole.ADMIN -> onNavigateToWelcome()
            }
            SessionCheck.LoggedOut -> onNavigateToWelcome()
            SessionCheck.Checking -> {}
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        SplashBackgroundWaves()

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = "file:///android_asset/Icon_Logo.png",
                contentDescription = null,
                modifier = Modifier.size(240.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.height(80.dp))
            SplashPagerIndicator()
            Spacer(Modifier.height(40.dp))
            CircularProgressIndicator(strokeWidth = 3.dp)
        }
    }
}

@Composable
private fun SplashBackgroundWaves() {
    val waveColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    val waveColorLight = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Top waves
        val topPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(width * 0.7f, 0f)
            quadraticTo(width * 0.3f, height * 0.1f, 0f, height * 0.25f)
            close()
        }
        drawPath(topPath, waveColor)

        val topPath2 = Path().apply {
            moveTo(0f, 0f)
            lineTo(width * 0.4f, 0f)
            quadraticTo(width * 0.1f, height * 0.15f, 0f, height * 0.35f)
            close()
        }
        drawPath(topPath2, waveColorLight)

        // Bottom waves
        val bottomPath = Path().apply {
            moveTo(width, height)
            lineTo(width * 0.3f, height)
            quadraticTo(width * 0.7f, height * 0.9f, width, height * 0.75f)
            close()
        }
        drawPath(bottomPath, waveColor)

        val bottomPath2 = Path().apply {
            moveTo(width, height)
            lineTo(width * 0.5f, height)
            quadraticTo(width * 0.85f, height * 0.85f, width, height * 0.65f)
            close()
        }
        drawPath(bottomPath2, waveColorLight)
    }
}

@Composable
private fun SplashPagerIndicator() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)))
        Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)))
    }
}
