package com.gymvip.mobile.presentation.screen.home

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.presentation.component.MembershipStatusCard
import com.gymvip.mobile.presentation.component.OfflineBanner
import com.gymvip.mobile.presentation.component.navigationIcon
import com.gymvip.mobile.presentation.navigation.MainTab
import com.gymvip.mobile.presentation.theme.LocalGymVipSystemTheme
import com.gymvip.mobile.presentation.viewmodel.HomeViewModel
import gymvip_mobile.composeapp.generated.resources.Res
import gymvip_mobile.composeapp.generated.resources.club_training
import org.jetbrains.compose.resources.painterResource
import com.gymvip.mobile.presentation.theme.accentForeground

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
        OfflineBanner(
            isOffline = state.isOffline,
            noticeType = state.noticeType,
            customMessage = state.errorMessage
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            val memberName = state.profile?.firstName ?: "Socio"
            if (LocalGymVipSystemTheme.current) {
                HomeWelcomeCard(
                    memberName = memberName,
                    onMembershipClick = { onNavigateTab(MainTab.MEMBERSHIP) }
                )
            } else {
                Text(
                    text = "¡Hola, $memberName!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            // Banner informativo (Firebase Remote Config acotado con fallback local)
            if (!state.remoteNoticeBanner.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Campaign,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.accentForeground,
                            modifier = Modifier.size(20.dp)
                        )
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
                text = "Estado de mi membresía",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            MembershipStatusCard(membership = state.membership)

            Spacer(modifier = Modifier.height(24.dp))

            // Accesos directos
            Text(
                text = "Accesos rápidos",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    title = "Mis pagos",
                    subtitle = "Historial en PEN",
                    onClick = { onNavigateTab(MainTab.MEMBERSHIP) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    icon = MainTab.CATALOG.navigationIcon,
                    title = "Catálogo",
                    subtitle = "Planes y suplementos",
                    onClick = { onNavigateTab(MainTab.CATALOG) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(
                    icon = MainTab.INBOX.navigationIcon,
                    title = "Avisos",
                    subtitle = "Comunicados del club",
                    onClick = { onNavigateTab(MainTab.INBOX) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    icon = MainTab.PROFILE.navigationIcon,
                    title = "Mi perfil",
                    subtitle = "Datos del titular",
                    onClick = { onNavigateTab(MainTab.PROFILE) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HomeWelcomeCard(
    memberName: String,
    onMembershipClick: () -> Unit
) {
    val charcoal = MaterialTheme.colorScheme.surfaceContainerLowest
    Box(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(charcoal)
            .heightIn(min = 186.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Image(
            painter = painterResource(Res.drawable.club_training),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.CenterEnd,
            modifier = Modifier.matchParentSize()
        )
        Box(
            modifier = Modifier.matchParentSize().background(
                Brush.horizontalGradient(
                    0f to charcoal.copy(alpha = 0.98f),
                    0.54f to charcoal.copy(alpha = 0.82f),
                    1f to charcoal.copy(alpha = 0.10f)
                )
            )
        )
        Column(
            modifier = Modifier.fillMaxWidth(0.64f).padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "¡Hola, $memberName!",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onMembershipClick) {
                Text("Mi membresía")
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(if (LocalGymVipSystemTheme.current) 20.dp else 12.dp)
    Card(
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = shape
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(
                modifier = Modifier.size(36.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(21.dp),
                        tint = MaterialTheme.colorScheme.accentForeground
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
