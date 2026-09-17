package com.nannyapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.nannyapp.domain.model.UserRole
import com.nannyapp.ui.admin.*
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

/**
 * Central Navigation system (request #37): Splash -> auth check -> role-scoped
 * home. Parent/Nanny/Admin each get their own bottom-nav shell (RoleScaffold)
 * hosting a nested NavHost so global destinations (messages/notifications/
 * profile/support) stay reachable from every role without duplicating routes.
 */
@Composable
fun NannyNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToWelcome = { navController.navigate(Routes.WELCOME) { popUpTo(Routes.SPLASH) { inclusive = true } } },
                onNavigateToParentHome = { navController.navigate(Routes.PARENT_DASHBOARD) { popUpTo(Routes.SPLASH) { inclusive = true } } },
                onNavigateToNannyHome = { navController.navigate(Routes.NANNY_DASHBOARD) { popUpTo(Routes.SPLASH) { inclusive = true } } },
                onNavigateToAdminHome = { navController.navigate(Routes.ADMIN_DASHBOARD) { popUpTo(Routes.SPLASH) { inclusive = true } } },
            )
        }

        composable(Routes.WELCOME) {
            WelcomeScreen(
                onFindCare = { navController.navigate(Routes.REGISTER) },
                onBecomeNanny = { navController.navigate(Routes.REGISTER) },
                onLogin = { navController.navigate(Routes.LOGIN) },
                onRegister = { navController.navigate(Routes.REGISTER) },
                onStaticPage = { key -> navController.navigate(Routes.staticPage(key)) },
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = { role ->
                    val dest = when (role) {
                        UserRole.PARENT -> Routes.PARENT_DASHBOARD
                        UserRole.NANNY -> Routes.NANNY_DASHBOARD
                        UserRole.ADMIN -> Routes.ADMIN_DASHBOARD
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

        composable(Routes.STATIC_PAGE, arguments = listOf(navArgument("pageKey") { type = NavType.StringType })) { entry ->
            val key = entry.arguments?.getString("pageKey") ?: ""
            StaticPageScreen(pageKey = key, onBack = { navController.popBackStack() })
        }

        // ---- Parent role shell ----
        composable(Routes.PARENT_DASHBOARD) { RoleScaffold(role = UserRole.PARENT, rootNav = navController) }

        // ---- Nanny role shell ----
        composable(Routes.NANNY_DASHBOARD) { RoleScaffold(role = UserRole.NANNY, rootNav = navController) }

        // ---- Admin role shell ----
        composable(Routes.ADMIN_DASHBOARD) { RoleScaffold(role = UserRole.ADMIN, rootNav = navController) }
    }
}
