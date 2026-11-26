package com.gymvip.mobile.presentation.component

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.presentation.theme.LocalGymVipSystemTheme
import com.gymvip.mobile.presentation.theme.accentForeground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VipTopAppBar(
    title: String,
    onOpenDrawer: (() -> Unit)? = null,
    onOpenInbox: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onSyncClick: (() -> Unit)? = null,
    isSyncing: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val containerColor = if (LocalGymVipSystemTheme.current) colors.surfaceContainerLowest else colors.surface

    TopAppBar(
        navigationIcon = {
            if (onOpenDrawer != null) {
                IconButton(onClick = onOpenDrawer, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Menu, contentDescription = "Abrir menú lateral", tint = colors.onSurface)
                }
            }
        },
        title = {
            Text(title, style = MaterialTheme.typography.titleLarge)
        },
        actions = {
            if (onOpenInbox != null) {
                IconButton(onClick = onOpenInbox, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Notificaciones y avisos", tint = colors.onSurface)
                }
            }
            if (onSyncClick != null) {
                IconButton(onClick = onSyncClick, modifier = Modifier.size(48.dp)) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = colors.accentForeground,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Sync,
                            contentDescription = "Sincronizar datos",
                            tint = colors.onSurface
                        )
                    }
                }
            }
            if (onOpenSettings != null) {
                IconButton(onClick = onOpenSettings, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Ajustes", tint = colors.onSurface)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            titleContentColor = colors.onSurface
        )
    )
}
