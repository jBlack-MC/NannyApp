package com.nannyapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    unreadNotifications: Int = 0,
    onNotificationsClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            if (title == "NannyApp" || title == "Nanny-App") NannyAppBrand()
            else Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1)
        },
        modifier = modifier,
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = {
            actions()
            if (onNotificationsClick != null) {
                IconButton(onClick = onNotificationsClick) {
                    BadgedBox(badge = { if (unreadNotifications > 0) Badge { Text(unreadNotifications.coerceAtMost(9).toString()) } }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val badgeCount: Int = 0,
)

@Composable
fun AppBottomNavigation(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        shadowElevation = 8.dp,
    ) {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            items.forEach { item ->
                NavigationBarItem(
                    selected = currentRoute == item.route,
                    onClick = { onItemClick(item) },
                    icon = {
                        BadgedBox(badge = { if (item.badgeCount > 0) Badge { Text(item.badgeCount.coerceAtMost(9).toString()) } }) {
                            Icon(item.icon, contentDescription = item.label)
                        }
                    },
                    label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
    }
}
