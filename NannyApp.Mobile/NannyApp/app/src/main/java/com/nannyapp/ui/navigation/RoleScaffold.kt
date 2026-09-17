package com.nannyapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nannyapp.domain.model.UserRole
import com.nannyapp.ui.components.BottomNavItem

/** Picks the correct bottom-nav shell for the logged-in role. `rootNav` is the
 * app-level NavController, used only to bounce back to Welcome/Login on logout. */
@Composable
fun RoleScaffold(role: UserRole, rootNav: NavHostController) {
    when (role) {
        UserRole.PARENT -> ParentAppScaffold(rootNav)
        UserRole.NANNY -> NannyAppScaffold(rootNav)
        UserRole.ADMIN -> AdminAppScaffold(rootNav)
    }
}

fun parentBottomItems() = listOf(
    BottomNavItem(Routes.PARENT_DASHBOARD, "Home", Icons.Filled.Home),
    BottomNavItem(Routes.FIND_NANNIES, "Find", Icons.Filled.Search),
    BottomNavItem(Routes.PARENT_BOOKINGS, "Bookings", Icons.Filled.CalendarMonth),
    BottomNavItem(Routes.MESSAGES, "Messages", Icons.Filled.ChatBubble),
    BottomNavItem(Routes.PROFILE, "Profile", Icons.Filled.Person),
)

fun nannyBottomItems() = listOf(
    BottomNavItem(Routes.NANNY_DASHBOARD, "Home", Icons.Filled.Home),
    BottomNavItem(Routes.NANNY_BOOKINGS, "Bookings", Icons.Filled.CalendarMonth),
    BottomNavItem(Routes.NANNY_EARNINGS, "Earnings", Icons.Filled.Payments),
    BottomNavItem(Routes.MESSAGES, "Messages", Icons.Filled.ChatBubble),
    BottomNavItem(Routes.PROFILE, "Profile", Icons.Filled.Person),
)

fun adminBottomItems() = listOf(
    BottomNavItem(Routes.ADMIN_DASHBOARD, "Home", Icons.Filled.Dashboard),
    BottomNavItem(Routes.ADMIN_BOOKINGS, "Bookings", Icons.Filled.CalendarMonth),
    BottomNavItem(Routes.ADMIN_PAYMENTS, "Payments", Icons.Filled.Payments),
    BottomNavItem(Routes.ADMIN_SUPPORT, "Support", Icons.Filled.SupportAgent),
    BottomNavItem(Routes.PROFILE, "Profile", Icons.Filled.Person),
)
