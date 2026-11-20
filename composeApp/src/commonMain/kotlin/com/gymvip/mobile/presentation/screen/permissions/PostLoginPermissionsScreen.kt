package com.gymvip.mobile.presentation.screen.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymvip.mobile.presentation.component.GymVipLogo
import com.gymvip.mobile.presentation.permission.DevicePermissionStatus
import com.gymvip.mobile.presentation.permission.rememberCameraPermission
import com.gymvip.mobile.presentation.permission.rememberNotificationPermission
import com.gymvip.mobile.presentation.theme.LocalGymVipSystemTheme
import com.gymvip.mobile.presentation.theme.SuccessGreen

@Composable
fun PostLoginPermissionsScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit = onContinue
) {
    val followsSystem = LocalGymVipSystemTheme.current
    val accent = if (followsSystem) MaterialTheme.colorScheme.primary else Color(0xFFD4AF37)

    val notifications = rememberNotificationPermission()
    val camera = rememberCameraPermission()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GymVipLogo(size = 64.dp, useGoldVariant = !followsSystem)

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Personaliza tu Experiencia",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Habilita solo las funciones que necesites. Puedes modificarlas en cualquier momento desde Ajustes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Tarjeta 1: Notificaciones
            PermissionOptionCard(
                icon = Icons.Outlined.Notifications,
                title = "Notificaciones y Avisos",
                description = "Recibe alertas preventivas antes de que venza tu membresía y comunicados oficiales del club.",
                isGranted = notifications.status == DevicePermissionStatus.ENABLED,
                accentColor = accent,
                onEnable = {
                    if (notifications.status == DevicePermissionStatus.CAN_REQUEST) {
                        notifications.request()
                    } else {
                        notifications.openSettings()
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjeta 2: Cámara para escáner
            PermissionOptionCard(
                icon = Icons.Outlined.PhotoCamera,
                title = "Cámara para Auto Check-in",
                description = "Escanea el código QR en el mostrador de recepción para registrar tu ingreso físico al gimnasio.",
                isGranted = camera.status == DevicePermissionStatus.ENABLED,
                accentColor = accent,
                onEnable = {
                    if (camera.status == DevicePermissionStatus.CAN_REQUEST) {
                        camera.request()
                    } else {
                        camera.openSettings()
                    }
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onContinue,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Continuar a GYM VIP", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onContinue,
                modifier = Modifier.height(40.dp)
            ) {
                Text(
                    text = "Omitir por ahora",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun PermissionOptionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    accentColor: Color,
    onEnable: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (isGranted) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Habilitado", style = MaterialTheme.typography.labelSmall, color = SuccessGreen, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            if (!isGranted) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onEnable,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                    modifier = Modifier.fillMaxWidth().height(38.dp)
                ) {
                    Text("Habilitar Permiso", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
