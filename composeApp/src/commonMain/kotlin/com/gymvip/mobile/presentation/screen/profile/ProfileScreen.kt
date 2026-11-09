package com.gymvip.mobile.presentation.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.presentation.theme.DangerRed
import com.gymvip.mobile.presentation.theme.GoldPrimary
import com.gymvip.mobile.presentation.theme.SuccessGreen
import com.gymvip.mobile.presentation.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    userRoles: List<String> = emptyList()
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        val profile = state.profile
        val roles = userRoles.ifEmpty { profile?.roles.orEmpty() }
        val accountType = when {
            roles.any { it.contains("ADMIN", ignoreCase = true) } -> "Administrador"
            roles.any { it.contains("RECEPCION", ignoreCase = true) } -> "Personal de recepción"
            roles.any { it.contains("CLIENT", ignoreCase = true) || it.contains("SOCIO", ignoreCase = true) || it.contains("MEMBER", ignoreCase = true) } -> "Socio / miembro"
            else -> "Socio / miembro"
        }
        val accountStatus = when (profile?.status?.name) {
            "BLOQUEADO" -> "CUENTA BLOQUEADA"
            "INACTIVO" -> "CUENTA INACTIVA"
            else -> "CUENTA ACTIVA"
        }

        // Header con Avatar VIP
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
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(GoldPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile?.firstName?.take(1)?.uppercase() ?: "S",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = profile?.fullName ?: "Socio GYM VIP",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = profile?.email ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SuccessGreen.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = accountStatus,
                        color = if (accountStatus == "CUENTA ACTIVA") SuccessGreen else DangerRed,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Tipo de cuenta: $accountType",
                    style = MaterialTheme.typography.labelMedium,
                    color = GoldPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Ficha de Datos Personales
        Text(
            text = "DATOS DEL TITULAR",
            style = MaterialTheme.typography.labelLarge,
            color = GoldPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ProfileDataRow("Documento:", "${profile?.documentType ?: "DNI"} ${profile?.documentNumber ?: "—"}")
                ProfileDataRow("Teléfono de contacto:", profile?.phone ?: "No registrado")
                ProfileDataRow("Identificador de cuenta:", profile?.accountId ?: "—")
                ProfileDataRow("Última sincronización:", profile?.lastSyncedAt?.take(19) ?: "Hoy")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botón de Cerrar Sesión seguro
        Button(
            onClick = { viewModel.logout() },
            colors = ButtonDefaults.buttonColors(
                containerColor = DangerRed.copy(alpha = 0.15f),
                contentColor = DangerRed
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(text = "🚪 Cerrar Sesión Segura", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ProfileDataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
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
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
