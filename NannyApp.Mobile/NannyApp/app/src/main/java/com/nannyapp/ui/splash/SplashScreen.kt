package com.nannyapp.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannyapp.domain.model.UserRole
import com.nannyapp.ui.auth.SessionCheck
import com.nannyapp.ui.auth.SplashViewModel
import com.nannyapp.ui.components.NannyAppBrand

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
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            NannyAppBrand(showTagline = true, light = true)
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator(color = Color.White)
        }
    }
}
