package com.nannyapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nannyapp.R
import androidx.navigation.NavHostController
import com.nannyapp.domain.model.UserRole
import com.nannyapp.ui.components.BottomNavItem

/** Picks the correct bottom-nav shell for the logged-in role. `rootNav` is the
 * app-level NavController, used only to bounce back to Welcome/Login on logout. */
@Composable
fun RoleScaffold(role: UserRole, rootNav: NavHostController) {
    when (role) {
        UserRole.PARENT -> ParentAppScaffold(rootNav)
        UserRole.NANNY -> NannyAppScaffold(rootNav)
        // Administrators use the web portal; this branch is only defensive
        // for an unexpected role value and has no mobile destination.
        UserRole.ADMIN -> Unit
    }
}

@Composable
fun parentBottomItems() = listOf(
    BottomNavItem(Routes.PARENT_DASHBOARD, stringResource(R.string.nav_home), Icons.Filled.Home),
    BottomNavItem(Routes.FIND_NANNIES, stringResource(R.string.nav_find), Icons.Filled.Search),
    BottomNavItem(Routes.PARENT_BOOKINGS, stringResource(R.string.nav_bookings), Icons.Filled.CalendarMonth),
    BottomNavItem(Routes.MESSAGES, stringResource(R.string.nav_messages), Icons.Filled.ChatBubble),
    BottomNavItem(Routes.PROFILE, stringResource(R.string.nav_profile), Icons.Filled.Person),
)

@Composable
fun nannyBottomItems() = listOf(
    BottomNavItem(Routes.NANNY_DASHBOARD, stringResource(R.string.nav_home), Icons.Filled.Home),
    BottomNavItem(Routes.NANNY_BOOKINGS, stringResource(R.string.nav_bookings), Icons.Filled.CalendarMonth),
    BottomNavItem(Routes.NANNY_EARNINGS, stringResource(R.string.nav_earnings), Icons.Filled.Payments),
    BottomNavItem(Routes.MESSAGES, stringResource(R.string.nav_messages), Icons.Filled.ChatBubble),
    BottomNavItem(Routes.PROFILE, stringResource(R.string.nav_profile), Icons.Filled.Person),
)
