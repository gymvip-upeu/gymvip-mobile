package pe.edu.upeu.gymvip.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import pe.edu.upeu.gymvip.domain.model.Favorite
import pe.edu.upeu.gymvip.domain.model.MembershipStatus
import pe.edu.upeu.gymvip.domain.model.MembershipSummary
import pe.edu.upeu.gymvip.domain.model.PaymentSummary
import pe.edu.upeu.gymvip.domain.model.Product
import pe.edu.upeu.gymvip.domain.model.SyncStatus
import pe.edu.upeu.gymvip.presentation.theme.DangerRed
import pe.edu.upeu.gymvip.presentation.theme.GoldOnPrimary
import pe.edu.upeu.gymvip.presentation.theme.GoldPrimary
import pe.edu.upeu.gymvip.presentation.theme.SuccessGreen
import pe.edu.upeu.gymvip.presentation.theme.WarningAmber
import pe.edu.upeu.gymvip.domain.model.fechaCortaSocio
import pe.edu.upeu.gymvip.domain.model.fechaPagoSocio
import pe.edu.upeu.gymvip.domain.model.etiqueta

@Composable
fun MembershipStatusCard(membership: MembershipSummary?, modifier: Modifier = Modifier) {
    if (membership == null) { EstadoVacio("Aún no tienes una membresía registrada. Acércate a recepción.", modifier); return }
    val vencida = membership.status == MembershipStatus.VENCIDA
    val acento = if (vencida) DangerRed else MaterialTheme.colorScheme.primary
    Column(modifier.fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
        .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(
            MaterialTheme.colorScheme.surfaceVariant, acento.copy(alpha = 0.20f), MaterialTheme.colorScheme.background)))
        .padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            LogoGV()
            VipStatusBadge(membership.status)
        }
        Spacer(Modifier.height(32.dp))
        Text("PLAN ACTUAL", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text(membership.planName, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(when {
            membership.status == MembershipStatus.CANCELADA -> "Tu membresía fue cancelada. Consulta en recepción."
            membership.status == MembershipStatus.PENDIENTE -> "Inicio previsto: " + fechaCortaSocio(membership.startDate)
            membership.status == MembershipStatus.DESCONOCIDO -> "No se pudo interpretar el estado enviado por el gimnasio."
            vencida -> "Renueva en recepción para volver a entrenar."
            membership.daysRemaining == 1 -> "1 día restante"
            membership.daysRemaining < 0 -> "Consulta la vigencia registrada en el detalle."
            else -> "${membership.daysRemaining} días restantes"
        }, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(32.dp))
        FilaDetalle(if (vencida) "Venció el" else "Último día válido", fechaCortaSocio(membership.endDate))
    }
}

@Composable
fun VipStatusBadge(status: MembershipStatus) {
    val oscuro = MaterialTheme.colorScheme.background.luminance() < .5f
    val colores = pe.edu.upeu.gymvip.presentation.theme.coloresEstado(status, oscuro)
    val bgColor = colores.first
    val textColor = colores.second
    val label = when (status) {
        MembershipStatus.VIGENTE -> "Membresía activa"
        MembershipStatus.PROXIMA_A_VENCER -> "Vence pronto"
        MembershipStatus.VENCIDA -> "Membresía vencida"
        MembershipStatus.CANCELADA -> "Membresía cancelada"
        MembershipStatus.PENDIENTE -> "Aún no inicia"
        MembershipStatus.DESCONOCIDO -> "Estado no disponible"
        MembershipStatus.SIN_MEMBRESIA -> "Sin membresía"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = label, color = textColor, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun PaymentItemCard(payment: PaymentSummary, modifier: Modifier = Modifier) {
    PanelSocio(modifier) {
        Text(payment.amount.amountInSoles, style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary)
        if (!payment.planName.isNullOrBlank()) Text(payment.planName, style = MaterialTheme.typography.titleMedium)
        Text(fechaPagoSocio(payment.paidAt), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(if (payment.paymentMethod == pe.edu.upeu.gymvip.domain.model.PaymentMethod.OTRO)
            payment.originalMethod?.takeIf { it.isNotBlank() } ?: payment.paymentMethod.etiqueta() else payment.paymentMethod.etiqueta())
        if (!payment.externalReference.isNullOrBlank()) FilaDetalle("Nº de operación", payment.externalReference)
        if (!payment.notes.isNullOrBlank()) Text(payment.notes, style = MaterialTheme.typography.bodyMedium)
        if (payment.state in setOf("REPLACED", "REEMPLAZADO"))
            Text("Reemplazado", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ProductItemCard(
    product: Product,
    onAddToFavorites: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (product.isPromotion) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GoldPrimary.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "-${product.promotionDiscountPercent ?: 10}% PROMO",
                            color = GoldPrimary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = product.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.price.amountInSoles,
                    style = MaterialTheme.typography.titleLarge,
                    color = GoldPrimary
                )

                Button(
                    onClick = { onAddToFavorites(product) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = GoldOnPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "⭐ Guardar Favorito", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun FavoriteItemCard(
    favorite: Favorite,
    onEditNote: (Favorite) -> Unit,
    onDelete: (Favorite) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = favorite.productName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SyncBadge(status = favorite.syncStatus)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Precio referencial: ${favorite.productPrice.amountInSoles}",
                style = MaterialTheme.typography.bodyMedium,
                color = GoldPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = "Mi nota personal:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = favorite.personalNote.ifBlank { "(Sin nota)" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = { onEditNote(favorite) },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "✏️ Editar Nota")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { onDelete(favorite) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "🗑️ Quitar")
                }
            }
        }
    }
}

@Composable
fun SyncBadge(status: SyncStatus) {
    val (text, color) = when (status) {
        SyncStatus.SYNCED -> "Sincronizado" to SuccessGreen
        SyncStatus.PENDING_CREATE -> "Pendiente subida" to WarningAmber
        SyncStatus.PENDING_UPDATE -> "Pendiente edición" to WarningAmber
        SyncStatus.PENDING_DELETE -> "Pendiente eliminar" to DangerRed
        SyncStatus.CONFLICT -> "Conflicto 409" to DangerRed
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = color, style = MaterialTheme.typography.labelSmall)
    }
}
