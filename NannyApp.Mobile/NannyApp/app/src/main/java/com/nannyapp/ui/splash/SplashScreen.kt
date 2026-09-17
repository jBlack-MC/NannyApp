package com.nannyapp.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nannyapp.domain.model.UserRole
import com.nannyapp.ui.auth.SessionCheck
import com.nannyapp.ui.auth.SplashViewModel

/** Splash screen per request #38: logo + tagline, then routes based on session state. */
@Composable
fun SplashScreen(
    onNavigateToWelcome: () -> Unit,
    onNavigateToParentHome: () -> Unit,
    onNavigateToNannyHome: () -> Unit,
    onNavigateToAdminHome: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        when (val s = state) {
            is SessionCheck.LoggedIn -> when (s.role) {
                UserRole.PARENT -> onNavigateToParentHome()
                UserRole.NANNY -> onNavigateToNannyHome()
                UserRole.ADMIN -> onNavigateToAdminHome()
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
            Box(
                modifier = Modifier.size(88.dp).background(Color.White, MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.ChildCare, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("NannyApp", color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Trusted childcare, on your schedule.", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(color = Color.White)
        }
    }
}
