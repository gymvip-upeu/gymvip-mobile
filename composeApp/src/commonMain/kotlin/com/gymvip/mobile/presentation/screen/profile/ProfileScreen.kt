package com.gymvip.mobile.presentation.screen.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.di.AppContainer
import com.gymvip.mobile.presentation.component.AvatarSelectionDialog
import com.gymvip.mobile.presentation.component.UserAvatar
import com.gymvip.mobile.presentation.theme.DangerRed
import com.gymvip.mobile.presentation.theme.GoldOnPrimary
import com.gymvip.mobile.presentation.theme.GoldPrimary
import com.gymvip.mobile.presentation.theme.LocalGymVipSystemTheme
import com.gymvip.mobile.presentation.theme.SuccessGreen
import com.gymvip.mobile.presentation.theme.accentForeground
import com.gymvip.mobile.presentation.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    userRoles: List<String> = emptyList(),
    onNavigateAdminScanner: () -> Unit = {}
) {
    val followsSystem = LocalGymVipSystemTheme.current
    val accent = if (followsSystem) MaterialTheme.colorScheme.primary else GoldPrimary
    val accentForeground = if (followsSystem) MaterialTheme.colorScheme.onSurface else GoldPrimary
    val onAccent = if (followsSystem) MaterialTheme.colorScheme.onPrimary else GoldOnPrimary
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val profile = state.profile
    val accountId = profile?.accountId.orEmpty()

    var avatarSeed by remember { mutableStateOf(accountId.ifBlank { "atleta_vip" }) }
    var showAvatarDialog by remember { mutableStateOf(false) }
    var showTechnicalDetails by remember { mutableStateOf(false) }

    // Cargar la semilla del avatar persistida por cuenta
    LaunchedEffect(accountId) {
        if (accountId.isNotBlank()) {
            val savedSeed = AppContainer.configRepository.getAvatarSeed(accountId)
            avatarSeed = savedSeed
        }
    }

    if (showAvatarDialog) {
        AvatarSelectionDialog(
            currentSeed = avatarSeed,
            onSelectAvatar = { newSeed ->
                avatarSeed = newSeed
                showAvatarDialog = false
                if (accountId.isNotBlank()) {
                    coroutineScope.launch {
                        AppContainer.configRepository.setAvatarSeed(accountId, newSeed)
                    }
                }
            },
            onDismiss = { showAvatarDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        val roles = userRoles.ifEmpty { profile?.roles.orEmpty() }
        val isAdminOrReception = roles.any {
            it.contains("ADMIN", ignoreCase = true) || it.contains("RECEPCION", ignoreCase = true)
        }
        val accountType = when {
            roles.any { it.contains("ADMIN", ignoreCase = true) } -> "Administrador del Sistema"
            roles.any { it.contains("RECEPCION", ignoreCase = true) } -> "Personal de Recepción"
            roles.any { it.contains("AUDITOR", ignoreCase = true) } -> "Auditor"
            else -> "Socio Titular VIP"
        }
        val accountStatus = when (profile?.status?.name) {
            "BLOQUEADO" -> "CUENTA BLOQUEADA"
            "INACTIVO" -> "CUENTA INACTIVA"
            else -> "CUENTA ACTIVA"
        }

        // Header con Avatar Creativo Local
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    UserAvatar(
                        seed = avatarSeed,
                        size = 84.dp,
                        onClick = { showAvatarDialog = true }
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(accent)
                            .clickable { showAvatarDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Cambiar avatar",
                            tint = onAccent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = profile?.fullName?.takeIf { it.isNotBlank() } ?: "Socio GYM VIP",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = profile?.email?.takeIf { it.isNotBlank() } ?: "Correo no registrado",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (accountStatus == "CUENTA ACTIVA") SuccessGreen.copy(alpha = 0.2f) else DangerRed.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = accountStatus,
                            color = if (accountStatus == "CUENTA ACTIVA") SuccessGreen else DangerRed,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accent.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = accountType,
                            color = accentForeground,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Ficha de Datos Personales
        Text(
            text = "DATOS DEL TITULAR",
            style = MaterialTheme.typography.labelLarge,
            color = accentForeground,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (!isAdminOrReception && !roles.contains("AUDITOR")) {
                    ProfileDataRow("Documento de Identidad:", "${profile?.documentType ?: "DNI"} ${profile?.documentNumber?.takeIf { it.isNotBlank() } ?: "No registrado"}")
                }
                ProfileDataRow("Teléfono de contacto:", profile?.phone?.takeIf { it.isNotBlank() } ?: "No registrado")
                ProfileDataRow("Última sincronización:", formatSyncDate(profile?.lastSyncedAt))
            }
        }

        // Acciones exclusivas para roles de gestión
        if (isAdminOrReception) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "HERRAMIENTAS DE GESTIÓN",
                style = MaterialTheme.typography.labelLarge,
                color = accentForeground,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onNavigateAdminScanner,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = onAccent
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Abrir Escáner de Productos y Stock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Sección Colapsable: Detalles Técnicos y Soporte
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTechnicalDetails = !showTechnicalDetails },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = null, tint = accentForeground, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Detalles Técnicos y Soporte",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(
                        imageVector = if (showTechnicalDetails) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(visible = showTechnicalDetails) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        ProfileDataRow("Identificador de cuenta (UUID):", profile?.accountId?.takeIf { it.isNotBlank() } ?: "No disponible")
                        ProfileDataRow("Versión de aplicación:", "GYM VIP Móvil v1.1.1 (Build 3)")
                        ProfileDataRow("Entorno de servidor:", "Producción Oficial (Render Cloud HTTPS)")
                        ProfileDataRow("Zona horaria oficial:", "America/Lima (UTC-5)")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

private fun formatSyncDate(dateString: String?): String {
    if (dateString.isNullOrBlank()) return "Sin sincronización confirmada"
    return runCatching {
        val instant = kotlinx.datetime.Instant.parse(dateString)
        val local = instant.toLocalDateTime(kotlinx.datetime.TimeZone.of("America/Lima"))
        "${local.toString().replace('T', ' ').take(16)} (America/Lima)"
    }.getOrElse { "Hora de sincronización no verificable" }
}

@Composable
private fun ProfileDataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}
