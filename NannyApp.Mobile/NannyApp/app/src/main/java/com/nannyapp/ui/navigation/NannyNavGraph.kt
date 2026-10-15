package com.nannyapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.nannyapp.domain.model.UserRole
import com.nannyapp.ui.auth.*
import com.nannyapp.ui.booking.*
import com.nannyapp.ui.messaging.ChatScreen
import com.nannyapp.ui.messaging.ConversationListScreen
import com.nannyapp.ui.misc.StaticPageScreen
import com.nannyapp.ui.nanny.*
import com.nannyapp.ui.notifications.NotificationsScreen
import com.nannyapp.ui.parent.*
import com.nannyapp.ui.profile.ProfileScreen
import com.nannyapp.ui.splash.SplashScreen
import com.nannyapp.ui.support.NewTicketScreen
import com.nannyapp.ui.support.SupportScreen
import com.nannyapp.ui.settings.SettingsScreen
import com.nannyapp.ui.safety.ReportScreen
import com.nannyapp.ui.plus.PlusScreen

/**
 * Central Navigation system (request #37): Splash -> auth check -> role-scoped
 * home. Parent and nanny accounts each get their own bottom-nav shell (RoleScaffold)
 * hosting a nested NavHost so global destinations (messages/notifications/
 * profile/support) stay reachable from every role without duplicating routes.
 */
@Composable
fun NannyNavGraph(sessionEvents: SessionEventsViewModel = hiltViewModel()) {
    val navController = rememberNavController()

    // A rejected sign-in has already been cleared; send the person to the login screen.
    LaunchedEffect(Unit) {
        sessionEvents.sessionExpired.collect {
            navController.navigate(Routes.LOGIN) { popUpTo(0) }
        }
    }

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToWelcome = { navController.navigate(Routes.WELCOME) { popUpTo(Routes.SPLASH) { inclusive = true } } },
                onNavigateToParentHome = { navController.navigate(Routes.PARENT_DASHBOARD) { popUpTo(Routes.SPLASH) { inclusive = true } } },
                onNavigateToNannyHome = { navController.navigate(Routes.NANNY_DASHBOARD) { popUpTo(Routes.SPLASH) { inclusive = true } } },
            )
        }

        composable(Routes.WELCOME) {
            WelcomeScreen(
                onFindCare = { navController.navigate(Routes.REGISTER) },
                onBecomeNanny = { navController.navigate(Routes.REGISTER) },
                onLogin = { navController.navigate(Routes.LOGIN) },
                onRegister = { navController.navigate(Routes.REGISTER) },
                onStaticPage = { key -> navController.navigate(Routes.staticPage(key)) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = { role ->
                    val dest = when (role) {
                        UserRole.PARENT -> Routes.PARENT_DASHBOARD
                        UserRole.NANNY -> Routes.NANNY_DASHBOARD
                        UserRole.ADMIN -> Routes.WELCOME
                    }
                    navController.navigate(dest) { popUpTo(Routes.WELCOME) { inclusive = true } }
                },
                onRegister = { navController.navigate(Routes.REGISTER) },
                onForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) },
                onVerifyEmail = { navController.navigate(Routes.verifyEmail()) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegistered = { navController.navigate(Routes.verifyEmail()) { popUpTo(Routes.WELCOME) } },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Routes.VERIFY_EMAIL,
            arguments = listOf(navArgument("token") { type = NavType.StringType; defaultValue = "" }),
            deepLinks = listOf(navDeepLink { uriPattern = "nannyapp://verify-email?token={token}" }),
        ) { entry ->
            val token = entry.arguments?.getString("token")
            VerifyEmailScreen(
                initialToken = token,
                onBack = { navController.popBackStack() },
                onVerified = { navController.navigate(Routes.SPLASH) { popUpTo(Routes.WELCOME) { inclusive = true } } },
                onSkip = { navController.navigate(Routes.SPLASH) { popUpTo(Routes.WELCOME) { inclusive = true } } },
            )
        }

        composable(Routes.PLUS) {
            PlusScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onStaticPage = { key -> navController.navigate(Routes.staticPage(key)) },
            )
        }

        composable(
            Routes.REPORT,
            arguments = listOf(
                navArgument("userId") { type = NavType.IntType },
                navArgument("name") { type = NavType.StringType; defaultValue = "" },
                navArgument("bookingId") { type = NavType.IntType; defaultValue = -1 },
            ),
        ) {
            ReportScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.STATIC_PAGE, arguments = listOf(navArgument("pageKey") { type = NavType.StringType })) { entry ->
            val key = entry.arguments?.getString("pageKey") ?: ""
            StaticPageScreen(pageKey = key, onBack = { navController.popBackStack() })
        }

        // ---- Parent role shell ----
        composable(Routes.PARENT_DASHBOARD) { RoleScaffold(role = UserRole.PARENT, rootNav = navController) }

        // ---- Nanny role shell ----
        composable(Routes.NANNY_DASHBOARD) { RoleScaffold(role = UserRole.NANNY, rootNav = navController) }

    }
}
