package com.gymvip.mobile.presentation.screen.permissions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.presentation.permission.DevicePermission
import com.gymvip.mobile.presentation.permission.DevicePermissionStatus
import com.gymvip.mobile.presentation.permission.rememberCameraPermission
import com.gymvip.mobile.presentation.permission.rememberNotificationPermission
import com.gymvip.mobile.presentation.screen.settings.SettingsPage

@Composable
fun AppPermissionsScreen(onBack: () -> Unit) {
    val notifications = rememberNotificationPermission()
    val camera = rememberCameraPermission()

    SettingsPage(title = "Permisos de la aplicación", onBack = onBack) {
        PermissionCard(
            title = "Notificaciones",
            description = "Activa las alertas en tu dispositivo. Aunque las desactives, tus avisos seguirán disponibles al actualizar la bandeja.",
            icon = Icons.Outlined.Notifications,
            permission = notifications
        )
        PermissionCard(
            title = "Cámara",
            description = "Permite escanear códigos de barras desde la aplicación.",
            icon = Icons.Outlined.PhotoCamera,
            permission = camera
        )
        if (camera.status != DevicePermissionStatus.UNAVAILABLE ||
            notifications.status != DevicePermissionStatus.UNAVAILABLE
        ) {
            Text(
                "Para desactivar un permiso, el interruptor te lleva a los ajustes de Android.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    icon: ImageVector,
    permission: DevicePermission
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (permission.status == DevicePermissionStatus.UNAVAILABLE) {
                        "No disponible en esta plataforma."
                    } else description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = permission.status == DevicePermissionStatus.ENABLED,
                onCheckedChange = if (permission.status == DevicePermissionStatus.UNAVAILABLE) null else {
                    {
                        if (permission.status == DevicePermissionStatus.CAN_REQUEST) permission.request()
                        else permission.openSettings()
                    }
                },
                enabled = permission.status != DevicePermissionStatus.UNAVAILABLE,
                modifier = Modifier.semantics { contentDescription = title }
            )
        }
    }
}
