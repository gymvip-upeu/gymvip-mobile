package com.gymvip.mobile.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SportsGymnastics
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.presentation.navigation.MainTab
import com.gymvip.mobile.presentation.navigation.NavDestination

@Composable
fun VipDrawerContent(
    memberName: String,
    memberEmail: String,
    preferredBranchName: String,
    isClient: Boolean,
    isStaff: Boolean,
    currentDestination: NavDestination,
    currentTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    onNavigate: (NavDestination) -> Unit,
    onLogout: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    val scroll = rememberScrollState()
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(vertical = 16.dp, horizontal = 12.dp)
        ) {
            Text(
                text = "GYM VIP",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Text(
                text = memberName.ifBlank { memberEmail },
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
            Text(
                text = preferredBranchName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            )
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            DrawerNavItem(
                label = "Inicio",
                icon = Icons.Outlined.Home,
                selected = currentDestination is NavDestination.Main && currentTab == MainTab.HOME,
                onClick = { onSelectTab(MainTab.HOME); onCloseDrawer() }
            )

            DrawerSectionHeader("GIMNASIOS")
            DrawerNavItem(
                label = "Explorar gimnasios y sedes",
                icon = Icons.Outlined.LocationOn,
                selected = currentDestination == NavDestination.GymExplorer,
                onClick = { onNavigate(NavDestination.GymExplorer); onCloseDrawer() }
            )

            if (isClient) {
                DrawerSectionHeader("MI MEMBRESÍA")
                DrawerNavItem(
                    label = "Estado de membresía y pagos",
                    icon = Icons.Outlined.Badge,
                    selected = currentDestination is NavDestination.Main && currentTab == MainTab.MEMBERSHIP,
                    onClick = { onSelectTab(MainTab.MEMBERSHIP); onCloseDrawer() }
                )
                DrawerNavItem(
                    label = "Escanear asistencia QR",
                    icon = Icons.Outlined.QrCodeScanner,
                    selected = currentDestination == NavDestination.MemberCheckInScanner,
                    onClick = { onNavigate(NavDestination.MemberCheckInScanner); onCloseDrawer() }
                )

                DrawerSectionHeader("ENTRENAMIENTO")
                DrawerNavItem(
                    label = "Mi rutina y sesión",
                    icon = Icons.Outlined.FitnessCenter,
                    selected = currentDestination is NavDestination.Main && currentTab == MainTab.TRAINING,
                    onClick = { onSelectTab(MainTab.TRAINING); onCloseDrawer() }
                )
                DrawerNavItem(
                    label = "Catálogo de ejercicios",
                    icon = Icons.Outlined.SportsGymnastics,
                    selected = currentDestination == NavDestination.ExerciseCatalog,
                    onClick = { onNavigate(NavDestination.ExerciseCatalog); onCloseDrawer() }
                )
                DrawerNavItem(
                    label = "Mi progreso",
                    icon = Icons.Outlined.BarChart,
                    selected = currentDestination == NavDestination.TrainingProgress,
                    onClick = { onNavigate(NavDestination.TrainingProgress); onCloseDrawer() }
                )

                DrawerNavItem(label = "Mi Bitácora", icon = Icons.Outlined.CalendarMonth,
                    selected = currentDestination == NavDestination.Journal,
                    onClick = { onNavigate(NavDestination.Journal); onCloseDrawer() })
                DrawerNavItem(label = "Perfil Físico y Plan", icon = Icons.Outlined.FitnessCenter,
                    selected = currentDestination == NavDestination.WellnessPlanner,
                    onClick = { onNavigate(NavDestination.WellnessPlanner); onCloseDrawer() })

                DrawerSectionHeader("RESERVAS")
                DrawerNavItem(
                    label = "Clases y reservas",
                    icon = Icons.Outlined.CalendarMonth,
                    selected = currentDestination == NavDestination.ClassReservations,
                    onClick = { onNavigate(NavDestination.ClassReservations); onCloseDrawer() }
                )
            }

            if (!isStaff || isClient) {
                DrawerSectionHeader("TIENDA")
                DrawerNavItem(
                    label = "Catálogo y favoritos",
                    icon = Icons.Outlined.ShoppingBag,
                    selected = currentDestination is NavDestination.Main && currentTab == MainTab.CATALOG,
                    onClick = { onSelectTab(MainTab.CATALOG); onCloseDrawer() }
                )
                if (isClient) {
                    DrawerNavItem(
                        label = "Carrito y mis pedidos",
                        icon = Icons.Outlined.ShoppingCart,
                        selected = currentDestination == NavDestination.Cart,
                        onClick = { onNavigate(NavDestination.Cart); onCloseDrawer() }
                    )
                }
            } else {
                DrawerSectionHeader("OPERACIONES STAFF")
                DrawerNavItem(
                    label = "Catálogo y stock",
                    icon = Icons.Outlined.ShoppingBag,
                    selected = currentDestination is NavDestination.Main && currentTab == MainTab.CATALOG,
                    onClick = { onSelectTab(MainTab.CATALOG); onCloseDrawer() }
                )
            }

            DrawerSectionHeader("COMUNICACIONES")
            DrawerNavItem(
                label = "Notificaciones y avisos",
                icon = Icons.Outlined.Notifications,
                selected = currentDestination is NavDestination.Main && currentTab == MainTab.INBOX,
                onClick = { onSelectTab(MainTab.INBOX); onCloseDrawer() }
            )
            DrawerNavItem(
                label = "Preguntas frecuentes",
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                selected = currentDestination == NavDestination.Faq,
                onClick = { onNavigate(NavDestination.Faq); onCloseDrawer() }
            )

            DrawerSectionHeader("MI CUENTA")
            DrawerNavItem(
                label = "Mi perfil",
                icon = Icons.Outlined.PersonOutline,
                selected = currentDestination is NavDestination.Main && currentTab == MainTab.PROFILE,
                onClick = { onSelectTab(MainTab.PROFILE); onCloseDrawer() }
            )
            DrawerNavItem(
                label = "Preferencias y guías",
                icon = Icons.Outlined.Tune,
                selected = currentDestination == NavDestination.Preferences,
                onClick = { onNavigate(NavDestination.Preferences); onCloseDrawer() }
            )
            DrawerNavItem(
                label = "Configuración y seguridad",
                icon = Icons.Outlined.Settings,
                selected = currentDestination == NavDestination.Settings,
                onClick = { onNavigate(NavDestination.Settings); onCloseDrawer() }
            )
            DrawerNavItem(
                label = "Cerrar sesión",
                icon = Icons.AutoMirrored.Outlined.ExitToApp,
                selected = false,
                onClick = { onCloseDrawer(); onLogout() }
            )
        }
    }
}

@Composable
private fun DrawerSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun DrawerNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null) },
        modifier = Modifier.padding(vertical = 1.dp)
    )
}
