package pe.edu.upeu.gymvip.presentation.component

import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import pe.edu.upeu.gymvip.presentation.navigation.MainTab
import pe.edu.upeu.gymvip.presentation.theme.GoldPrimary

@Composable
fun VipNavigationBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    avisosSinLeer: Int = 0
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        MainTab.entries.forEach { tab ->
            val isSelected = currentTab == tab
            val icon = when (tab) {
                MainTab.HOME -> Icons.Outlined.Home
                MainTab.MEMBERSHIP -> Icons.Outlined.CreditCard
                MainTab.CATALOG -> Icons.Outlined.GridView
                MainTab.INBOX -> Icons.Outlined.Notifications
                MainTab.PROFILE -> Icons.Outlined.PersonOutline
            }

            NavigationBarItem(
                modifier = Modifier.semantics { if (tab == MainTab.INBOX) stateDescription = "$avisosSinLeer avisos sin leer" },
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    BadgedBox(badge = {
                        if (tab == MainTab.INBOX && avisosSinLeer > 0) Badge(containerColor = MaterialTheme.colorScheme.primary)
                    }) { Icon(icon, contentDescription = null) }
                },
                label = {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.surface,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
