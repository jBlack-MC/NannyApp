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
import com.nannyapp.ui.notifications.NotificationsScreen
import com.nannyapp.ui.parent.*
import com.nannyapp.ui.profile.ProfileScreen
import com.nannyapp.ui.support.NewTicketScreen
import com.nannyapp.ui.support.SupportScreen

@Composable
fun ParentAppScaffold(rootNav: NavHostController) {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val tabRoutes = parentBottomItems().map { it.route }.toSet()

    Scaffold(
        bottomBar = {
            if (currentRoute in tabRoutes) {
                AppBottomNavigation(
                    items = parentBottomItems(), currentRoute = currentRoute,
                    onItemClick = { item -> nav.navigate(item.route) { popUpTo(Routes.PARENT_DASHBOARD) { saveState = true }; launchSingleTop = true; restoreState = true } },
                )
            }
        },
    ) { padding ->
        NavHost(navController = nav, startDestination = Routes.PARENT_DASHBOARD, modifier = androidx.compose.ui.Modifier.padding(padding)) {
            composable(Routes.PARENT_DASHBOARD) {
                ParentDashboardScreen(
                    onFindNannies = { nav.navigate(Routes.FIND_NANNIES) },
                    onNannyClick = { id -> nav.navigate(Routes.nannyDetail(id)) },
                    onBookingClick = { id -> nav.navigate(Routes.bookingDetail(id)) },
                    onSaved = { nav.navigate(Routes.SAVED_NANNIES) },
                    onChildren = { nav.navigate(Routes.CHILDREN) },
                    onPayments = { nav.navigate(Routes.PARENT_PAYMENTS) },
                    onMessages = { nav.navigate(Routes.MESSAGES) },
                    onNotifications = { nav.navigate(Routes.NOTIFICATIONS) },
                )
            }
            composable(Routes.FIND_NANNIES) {
                FindNanniesScreen(onNannyClick = { id -> nav.navigate(Routes.nannyDetail(id)) }, onBack = { nav.popBackStack() })
            }
            composable(Routes.NANNY_DETAIL, arguments = listOf(navArgument("nannyId") { type = NavType.IntType })) {
                NannyDetailScreen(
                    onBack = { nav.popBackStack() },
                    onBookNow = { id -> nav.navigate(Routes.bookingWizard(id)) },
                    onMessage = { id, name -> nav.navigate(Routes.chat(id, name)) },
                )
            }
            composable(Routes.SAVED_NANNIES) {
                SavedNanniesScreen(onBack = { nav.popBackStack() }, onNannyClick = { id -> nav.navigate(Routes.nannyDetail(id)) })
            }
            composable(Routes.CHILDREN) {
                ChildrenScreen(
                    onBack = { nav.popBackStack() },
                    onAddChild = { nav.navigate(Routes.childForm()) },
                    onEditChild = { id -> nav.navigate(Routes.childForm(id)) },
                )
            }
            composable(
                Routes.CHILD_FORM,
                arguments = listOf(navArgument("childId") { type = NavType.IntType; defaultValue = -1 }),
            ) {
                ChildFormScreen(onSaved = { nav.popBackStack() }, onBack = { nav.popBackStack() })
            }
            composable(Routes.PARENT_BOOKINGS) {
                ParentBookingsScreen(onBookingClick = { id -> nav.navigate(Routes.bookingDetail(id)) })
            }
            composable(Routes.PARENT_PAYMENTS) {
                ParentPaymentsScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.BOOKING_WIZARD, arguments = listOf(navArgument("nannyId") { type = NavType.IntType })) {
                BookingWizardScreen(
                    onBack = { nav.popBackStack() },
                    onBookingCreated = { nav.navigate(Routes.PARENT_BOOKINGS) { popUpTo(Routes.PARENT_DASHBOARD) } },
                )
            }
            composable(Routes.BOOKING_DETAIL, arguments = listOf(navArgument("bookingId") { type = NavType.IntType })) {
                BookingDetailScreen(
                    onBack = { nav.popBackStack() },
                    onCheckIn = { },
                    onLeaveReview = { id -> nav.navigate(Routes.leaveReview(id)) },
                )
            }
            composable(Routes.LEAVE_REVIEW, arguments = listOf(navArgument("bookingId") { type = NavType.IntType })) {
                LeaveReviewScreen(onBack = { nav.popBackStack() }, onSubmitted = { nav.popBackStack() })
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
                ProfileScreen(
                    onLoggedOut = { rootNav.navigate(Routes.WELCOME) { popUpTo(0) } },
                    onSupport = { nav.navigate(Routes.SUPPORT) },
                )
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
