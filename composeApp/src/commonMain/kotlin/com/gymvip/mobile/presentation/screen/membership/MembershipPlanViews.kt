package com.gymvip.mobile.presentation.screen.membership

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.domain.model.MembershipSummary
import com.gymvip.mobile.domain.model.MoneyPen
import com.gymvip.mobile.presentation.component.GymVipLogo
import com.gymvip.mobile.presentation.theme.GoldPrimary
import com.gymvip.mobile.presentation.viewmodel.MembershipUiState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

internal fun membershipDurationDays(membership: MembershipSummary): Int? =
    membership.durationDays?.takeIf { it > 0 } ?: runCatching {
        (LocalDate.parse(membership.startDate).daysUntil(LocalDate.parse(membership.endDate)) + 1).takeIf { it > 0 }
    }.getOrNull()

internal fun membershipDate(value: String): String = runCatching {
    val date = LocalDate.parse(value)
    val month = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")[date.monthNumber - 1]
    "${date.dayOfMonth} $month ${date.year}"
}.getOrElse { "No registrado" }

@Composable
internal fun CurrentMembershipCard(membership: MembershipSummary) {
    val active = membership.isAccessAllowed
    val colors = MaterialTheme.colorScheme
    Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(colors.surfaceVariant, colors.surface)))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                GymVipLogo(size = 34.dp, useGoldVariant = true)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (active) colors.secondaryContainer else colors.errorContainer,
                    contentColor = if (active) colors.onSecondaryContainer else colors.onErrorContainer
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (active) {
                            Icon(Icons.Outlined.Check, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(if (active) "Membresía activa" else "Membresía vencida", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Plan actual", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                Text(membership.planName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = colors.onSurface)
                Text(
                    when {
                        !active -> "Consulta en recepción para renovar tu acceso."
                        membership.daysRemaining <= 0 -> "Hoy es tu último día válido"
                        membership.daysRemaining == 1 -> "1 día restante"
                        else -> "${membership.daysRemaining} días restantes"
                    }, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Último día válido", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                Text(membershipDate(membership.endDate).uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.onSurface)
            }
        }
    }
}

@Composable
internal fun MembershipContractDetails(membership: MembershipSummary, paymentMethod: String?, onOpenPayments: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            ContractRow("Inicio del período", membershipDate(membership.startDate))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ContractRow("Último día válido", membershipDate(membership.endDate))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ContractRow("Duración", membershipDurationDays(membership)?.let { "$it días calendario" } ?: "Consultar recepción")
            if (paymentMethod != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ContractRow("Medio de pago registrado", paymentMethod)
            }
        }
    }
    if (!membership.benefits.isNullOrBlank()) {
        Text("Beneficios de tu plan", style = MaterialTheme.typography.titleMedium)
        Text(membership.benefits, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (!membership.conditions.isNullOrBlank()) {
        Text("Condiciones de tu plan", style = MaterialTheme.typography.titleMedium)
        Text(membership.conditions, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    OutlinedCard(onClick = onOpenPayments, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Historial de pagos", style = MaterialTheme.typography.titleMedium)
                Text("Importes y medios registrados", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = GoldPrimary)
        }
    }
}

@Composable
private fun ContractRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MembershipPlansSheet(state: MembershipUiState, onDismiss: () -> Unit, onRetry: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Planes y precios", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "Cerrar planes") }
                }
                Text("Elige el que va contigo. La contratación y renovación se realizan en recepción.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            when {
                state.plansLoading -> item {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CircularProgressIndicator()
                        Text("Consultando planes disponibles…")
                    }
                }
                state.plansError != null -> item {
                    Text("No se pudieron consultar los planes", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(state.plansError, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) { Text("Reintentar") }
                }
                else -> {
                    if (state.plansCached) item {
                        Text("Sin conexión con el gimnasio. Estos son los últimos planes guardados; confirma precios y disponibilidad en recepción.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.plans.isEmpty()) item {
                        Text("No hay planes publicados por el momento.", style = MaterialTheme.typography.titleMedium)
                        Text("Consulta las opciones disponibles en recepción.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(state.plans, key = { it.id }) { plan ->
                        OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(plan.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("${plan.durationDays} días calendario", Modifier.weight(1f).padding(end = 12.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(MoneyPen.fromDouble(plan.pricePen).amountInSoles, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                }
                                plan.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                                plan.benefits?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                plan.conditions?.takeIf { it.isNotBlank() }?.let {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
