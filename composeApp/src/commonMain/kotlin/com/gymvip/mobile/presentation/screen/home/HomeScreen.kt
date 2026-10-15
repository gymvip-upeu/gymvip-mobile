package com.gymvip.mobile.presentation.screen.home

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.presentation.component.MembershipStatusCard
import com.gymvip.mobile.presentation.component.OfflineBanner
import com.gymvip.mobile.presentation.navigation.MainTab
import com.gymvip.mobile.presentation.theme.GoldPrimary
import com.gymvip.mobile.presentation.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateTab: (MainTab) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        OfflineBanner(isOffline = state.isOffline)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Saludo personalizado al socio
            val memberName = state.profile?.firstName ?: "Socio"
            Text(
                text = "¡Hola, $memberName!",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Bienvenido a su centro de control personal GYM VIP.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Banner informativo (Firebase Remote Config acotado con fallback local)
            if (!state.remoteNoticeBanner.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GoldPrimary.copy(alpha = 0.15f))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📢", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text(
                            text = state.remoteNoticeBanner ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Tarjeta principal de membresía activa (con regla estricta de 30 días)
            Text(
                text = "ESTADO DE MI MEMBRESÍA",
                style = MaterialTheme.typography.labelLarge,
                color = GoldPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            MembershipStatusCard(membership = state.membership)

            Spacer(modifier = Modifier.height(24.dp))

            // Accesos directos
            Text(
                text = "ACCESOS RÁPIDOS",
                style = MaterialTheme.typography.labelLarge,
                color = GoldPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(
                    icon = "💳",
                    title = "Mis Pagos",
                    subtitle = "Historial en PEN",
                    onClick = { onNavigateTab(MainTab.MEMBERSHIP) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    icon = "📋",
                    title = "Catálogo",
                    subtitle = "Planes y suplementos",
                    onClick = { onNavigateTab(MainTab.CATALOG) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(
                    icon = "🔔",
                    title = "Avisos",
                    subtitle = "Comunicados del club",
                    onClick = { onNavigateTab(MainTab.INBOX) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    icon = "👤",
                    title = "Mi Perfil",
                    subtitle = "Datos del titular",
                    onClick = { onNavigateTab(MainTab.PROFILE) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = icon, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
