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
import com.nannyapp.ui.booking.*
import com.nannyapp.ui.components.AppBottomNavigation
import com.nannyapp.ui.messaging.ChatScreen
import com.nannyapp.ui.messaging.ConversationListScreen
import com.nannyapp.ui.nanny.*
import com.nannyapp.ui.notifications.NotificationsScreen
import com.nannyapp.ui.profile.ProfileScreen
import com.nannyapp.ui.support.NewTicketScreen
import com.nannyapp.ui.support.SupportScreen

@Composable
fun NannyAppScaffold(rootNav: NavHostController) {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val tabRoutes = nannyBottomItems().map { it.route }.toSet()

    Scaffold(
        bottomBar = {
            if (currentRoute in tabRoutes) {
                AppBottomNavigation(
                    items = nannyBottomItems(), currentRoute = currentRoute,
                    onItemClick = { item -> nav.navigate(item.route) { popUpTo(Routes.NANNY_DASHBOARD) { saveState = true }; launchSingleTop = true; restoreState = true } },
                )
            }
        },
    ) { padding ->
        NavHost(navController = nav, startDestination = Routes.NANNY_DASHBOARD, modifier = androidx.compose.ui.Modifier.padding(padding)) {
            composable(Routes.NANNY_DASHBOARD) {
                NannyDashboardScreen(
                    onBookingClick = { id -> nav.navigate(Routes.bookingDetail(id)) },
                    onManageBookings = { nav.navigate(Routes.NANNY_BOOKINGS) },
                    onAvailability = { nav.navigate(Routes.NANNY_AVAILABILITY) },
                    onEditProfile = { nav.navigate(Routes.NANNY_PROFILE_EDIT) },
                    onEarnings = { nav.navigate(Routes.NANNY_EARNINGS) },
                    onNotifications = { nav.navigate(Routes.NOTIFICATIONS) },
                )
            }
            composable(Routes.NANNY_BOOKINGS) {
                NannyBookingsScreen(onBookingClick = { id -> nav.navigate(Routes.bookingDetail(id)) })
            }
            composable(Routes.BOOKING_DETAIL, arguments = listOf(navArgument("bookingId") { type = NavType.IntType })) {
                BookingDetailScreen(
                    onBack = { nav.popBackStack() },
                    onCheckIn = { id -> nav.navigate(Routes.checkIn(id)) },
                    onLeaveReview = { },
                )
            }
            composable(Routes.CHECK_IN, arguments = listOf(navArgument("bookingId") { type = NavType.IntType })) {
                CheckInScreen(onBack = { nav.popBackStack() }, onCheckedIn = { nav.popBackStack() })
            }
            composable(Routes.NANNY_AVAILABILITY) {
                NannyAvailabilityScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.NANNY_EARNINGS) {
                NannyEarningsScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.NANNY_REVIEWS) {
                NannyReviewsScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.NANNY_PROFILE_EDIT) {
                NannyProfileEditScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.NANNY_PORTFOLIO) {
                NannyPortfolioScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.MESSAGES) {
                ConversationListScreen(onConversationClick = { id, name -> nav.navigate(Routes.chat(id, name)) })
            }
            composable(
                Routes.CHAT,
                arguments = listOf(
                    navArgument("userId") { type = NavType.IntType },
                    navArgument("peerName") { type = NavType.StringType; defaultValue = "" },
                ),
            ) {
                ChatScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.PROFILE) {
                ProfileScreen(onLoggedOut = { rootNav.navigate(Routes.WELCOME) { popUpTo(0) } })
            }
            composable(Routes.SUPPORT) {
                SupportScreen(onNewTicket = { nav.navigate(Routes.SUPPORT_NEW_TICKET) })
            }
            composable(Routes.SUPPORT_NEW_TICKET) {
                NewTicketScreen(onBack = { nav.popBackStack() }, onSubmitted = { nav.popBackStack() })
            }
        }
    }
}
