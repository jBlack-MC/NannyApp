package com.nannyapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nannyapp.ui.admin.*
import com.nannyapp.ui.booking.BookingDetailScreen
import com.nannyapp.ui.components.AppBottomNavigation
import com.nannyapp.ui.messaging.ChatScreen
import com.nannyapp.ui.messaging.ConversationListScreen
import com.nannyapp.ui.notifications.NotificationsScreen
import com.nannyapp.ui.profile.ProfileScreen

@Composable
fun AdminAppScaffold(rootNav: NavHostController) {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val tabRoutes = adminBottomItems().map { it.route }.toSet()

    Scaffold(
        bottomBar = {
            if (currentRoute in tabRoutes) {
                AppBottomNavigation(
                    items = adminBottomItems(), currentRoute = currentRoute,
                    onItemClick = { item -> nav.navigate(item.route) { popUpTo(Routes.ADMIN_DASHBOARD) { saveState = true }; launchSingleTop = true; restoreState = true } },
                )
            }
        },
    ) { padding ->
        NavHost(navController = nav, startDestination = Routes.ADMIN_DASHBOARD, modifier = androidx.compose.ui.Modifier.padding(padding)) {
            composable(Routes.ADMIN_DASHBOARD) {
                AdminDashboardScreen(
                    onUsers = { nav.navigate(Routes.ADMIN_USERS) },
                    onVerifications = { nav.navigate(Routes.ADMIN_VERIFICATIONS) },
                    onBookings = { nav.navigate(Routes.ADMIN_BOOKINGS) },
                    onPayments = { nav.navigate(Routes.ADMIN_PAYMENTS) },
                    onReports = { nav.navigate(Routes.ADMIN_REPORTS) },
                    onSupport = { nav.navigate(Routes.ADMIN_SUPPORT) },
                )
            }
            composable(Routes.ADMIN_USERS) { AdminUsersScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.ADMIN_VERIFICATIONS) { AdminVerificationsScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.ADMIN_BOOKINGS) {
                AdminBookingsScreen(onBack = { nav.popBackStack() }, onBookingClick = { id -> nav.navigate(Routes.bookingDetail(id)) })
            }
            composable(Routes.BOOKING_DETAIL, arguments = listOf(navArgument("bookingId") { type = NavType.IntType })) {
                BookingDetailScreen(onBack = { nav.popBackStack() }, onCheckIn = { }, onLeaveReview = { })
            }
            composable(Routes.ADMIN_PAYMENTS) { AdminPaymentsScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.ADMIN_REPORTS) { AdminReportsScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.ADMIN_SUPPORT) { AdminSupportScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.MESSAGES) { ConversationListScreen(onConversationClick = { id, name -> nav.navigate(Routes.chat(id, name)) }) }
            composable(
                Routes.CHAT,
                arguments = listOf(
                    navArgument("userId") { type = NavType.IntType },
                    navArgument("peerName") { type = NavType.StringType; defaultValue = "" },
                ),
            ) {
                ChatScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.NOTIFICATIONS) { NotificationsScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.PROFILE) {
                ProfileScreen(onLoggedOut = { rootNav.navigate(Routes.WELCOME) { popUpTo(0) } })
            }
        }
    }
}
