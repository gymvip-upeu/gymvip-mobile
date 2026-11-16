package com.gymvip.mobile.presentation.screen.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.presentation.permission.NotificationPermission
import com.gymvip.mobile.presentation.permission.NotificationPermissionStatus
import com.gymvip.mobile.presentation.permission.rememberNotificationPermission

@Composable
fun AppPermissionsScreen(onBack: () -> Unit) {
    val permission = rememberNotificationPermission()

    Column(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Permisos de la aplicación",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp)
        ) {
            NotificationPermissionCard(permission)
        }
    }
}

@Composable
internal fun NotificationPermissionCard(permission: NotificationPermission) {
    val description = when (permission.status) {
        NotificationPermissionStatus.CAN_REQUEST ->
            "Si activas las notificaciones, GYM VIP podrá mostrar avisos en tu celular."
        NotificationPermissionStatus.DISABLED ->
            "Las notificaciones están desactivadas. Puedes seguir consultando tus avisos aquí."
        NotificationPermissionStatus.ENABLED ->
            "Las notificaciones están permitidas en este dispositivo."
        NotificationPermissionStatus.UNAVAILABLE ->
            "Puedes gestionar este permiso desde la app de Android."
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Notificaciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            when (permission.status) {
                NotificationPermissionStatus.CAN_REQUEST ->
                    Button(onClick = permission.request) { Text("Activar notificaciones") }
                NotificationPermissionStatus.DISABLED, NotificationPermissionStatus.ENABLED ->
                    OutlinedButton(onClick = permission.openSettings) { Text("Abrir ajustes") }
                NotificationPermissionStatus.UNAVAILABLE -> Unit
            }
        }
    }
}
