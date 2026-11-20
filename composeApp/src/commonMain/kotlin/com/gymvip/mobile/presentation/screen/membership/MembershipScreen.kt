package com.gymvip.mobile.presentation.screen.membership

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CardMembership
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ShoppingBag

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.domain.model.MembershipStatus
import com.gymvip.mobile.presentation.component.ContextualHelpCard
import com.gymvip.mobile.presentation.component.EmptyView
import com.gymvip.mobile.presentation.component.LoadingView
import com.gymvip.mobile.presentation.component.MembershipStatusCard
import com.gymvip.mobile.presentation.component.OfflineBanner
import com.gymvip.mobile.presentation.component.PaymentItemCard
import com.gymvip.mobile.presentation.theme.GoldPrimary
import com.gymvip.mobile.presentation.theme.WarningAmber
import com.gymvip.mobile.presentation.theme.accentForeground
import com.gymvip.mobile.presentation.viewmodel.MembershipViewModel

@Composable
fun MembershipScreen(
    viewModel: MembershipViewModel,
    userRoles: List<String> = emptyList(),
    onNavigateToCatalog: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val isStaff = userRoles.any { it.equals("ADMIN", ignoreCase = true) || it.equals("RECEPCION", ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        OfflineBanner(isOffline = state.isOffline)

        // Ayuda contextual opcional y no invasiva para la sección de membresía
        ContextualHelpCard(
            screenKey = "membership_help",
            title = "Membresía y Vigencia",
            description = "Consulte la vigencia de 30 días calendario y su historial de pagos en PEN. Las renovaciones se realizan exclusivamente en el mostrador de recepción."
        )

        TabRow(
            selectedTabIndex = state.selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.accentForeground,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[state.selectedSubTab]),
                    color = MaterialTheme.colorScheme.accentForeground
                )
            }
        ) {
            Tab(
                selected = state.selectedSubTab == 0,
                onClick = { viewModel.setSubTab(0) },
                text = { Text("Vigencia Actual", style = MaterialTheme.typography.titleSmall) }
            )
            Tab(
                selected = state.selectedSubTab == 1,
                onClick = { viewModel.setSubTab(1) },
                text = { Text("Historial de Pagos (PEN)", style = MaterialTheme.typography.titleSmall) }
            )
        }

        if (state.isLoading && state.membership == null && state.payments.isEmpty()) {
            LoadingView("Consultando datos de membresía...")
        } else if (state.errorMessage != null && state.membership == null && !isStaff) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudOff,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No se pudieron obtener los datos de su membresía",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.errorMessage ?: "Ocurrió un error al contactar al servidor GYM VIP.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { viewModel.refresh() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Reintentar conexión")
                }
            }
        } else {
            when (state.selectedSubTab) {
                0 -> MembershipDetailTab(
                    state = state,
                    isStaff = isStaff,
                    userRoles = userRoles,
                    onNavigateToCatalog = onNavigateToCatalog
                )
                1 -> PaymentHistoryTab(
                    state = state,
                    isStaff = isStaff
                )
            }
        }
    }
}

@Composable
private fun MembershipDetailTab(
    state: com.gymvip.mobile.presentation.viewmodel.MembershipUiState,
    isStaff: Boolean,
    userRoles: List<String>,
    onNavigateToCatalog: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (isStaff) {
            AdminAccountMembershipNotice(userRoles = userRoles)
        } else {
            val mem = state.membership
            val hasActiveMembership = mem != null &&
                    mem.status != MembershipStatus.SIN_MEMBRESIA &&
                    mem.daysRemaining > 0

            if (state.isOffline && mem != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(WarningAmber.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Mostrando datos de membresía de la última sesión sincronizada.",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarningAmber
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (hasActiveMembership) {
                MembershipStatusCard(membership = mem)
            } else {
                NoActiveMembershipCard(onNavigateToCatalog = onNavigateToCatalog)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Reglamento y Condiciones de Membresía",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Cada mensualidad otorga exactamente 30 días calendario continuos de acceso.\n" +
                            "• No se aplican días de gracia ni extensiones automáticas (cero tolerancia).\n" +
                            "• Las renovaciones y pagos se tramitan exclusivamente de forma presencial en recepción de sede Ñaña.\n" +
                            "• Su historial permanece accesible en todo momento para su control contable.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AdminAccountMembershipNotice(userRoles: List<String>) {
    val roleTitle = if (userRoles.any { it.equals("ADMIN", ignoreCase = true) }) "Administrador" else "Personal de Recepción"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GoldPrimary.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GoldPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AdminPanelSettings,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column {
                    Text(
                        text = "Cuenta de Gestión: $roleTitle",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Sede Central Ñaña · Credenciales de Servicio",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.accentForeground
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Como integrante del equipo de administración o recepción, su cuenta dispone de facultades operativas y de control. El personal no requiere la contratación de una membresía de socio para acceder a los servicios de la plataforma.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Para verificar socios o consultar inventario, utilice los accesos rápidos desde la pantalla de inicio o perfil.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NoActiveMembershipCard(onNavigateToCatalog: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CardMembership,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.accentForeground,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Sin Membresía Activa",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Actualmente no cuenta con un plan vigente en GYM VIP. Para adquirir o renovar su acceso, acérquese al mostrador de recepción en sede Ñaña.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onNavigateToCatalog,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Outlined.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("Ver Planes y Precios Disponibles")
            }
        }
    }
}

@Composable
private fun PaymentHistoryTab(
    state: com.gymvip.mobile.presentation.viewmodel.MembershipUiState,
    isStaff: Boolean
) {
    if (isStaff) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "El registro contable de pagos de socios se administra a través del panel de recepción de GYM VIP.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else if (state.payments.isEmpty()) {
        EmptyView(
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            title = "Sin pagos registrados",
            message = "No se encontraron recibos de pago en su cuenta de socio."
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.payments, key = { it.id }) { payment ->
                PaymentItemCard(payment = payment)
            }
        }
    }
}
