package com.gymvip.mobile.presentation.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.presentation.navigation.MainTab
import com.gymvip.mobile.presentation.theme.LocalGymVipSystemTheme

internal val MainTab.navigationIcon: ImageVector
    get() = when (this) {
        MainTab.HOME -> Icons.Outlined.Home
        MainTab.MEMBERSHIP -> Icons.Outlined.Badge
        MainTab.TRAINING -> Icons.Outlined.FitnessCenter
        MainTab.CATALOG -> Icons.Outlined.ShoppingBag
        MainTab.INBOX -> Icons.Outlined.Notifications
        MainTab.PROFILE -> Icons.Outlined.PersonOutline
    }

@Composable
fun VipNavigationBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
    visibleTabs: List<MainTab> = listOf(MainTab.HOME, MainTab.MEMBERSHIP, MainTab.TRAINING, MainTab.CATALOG, MainTab.PROFILE)
) {
    val colors = MaterialTheme.colorScheme
    val followsSystem = LocalGymVipSystemTheme.current
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp
    ) {
        visibleTabs.take(5).forEach { tab ->
            val isSelected = currentTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.navigationIcon,
                        contentDescription = null
                    )
                },
                label = {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = if (followsSystem) colors.onPrimary else colors.onPrimaryContainer,
                    selectedTextColor = colors.onSurface,
                    indicatorColor = if (followsSystem) colors.primary else colors.primaryContainer,
                    unselectedIconColor = colors.onSurfaceVariant,
                    unselectedTextColor = colors.onSurfaceVariant
                )
            )
        }
    }
}
