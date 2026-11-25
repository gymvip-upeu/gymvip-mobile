package com.gymvip.mobile.presentation.screen.inbox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.domain.model.InboxCategory
import com.gymvip.mobile.domain.model.InboxItem
import com.gymvip.mobile.presentation.component.ContextualHelpCard
import com.gymvip.mobile.presentation.component.EmptyView
import com.gymvip.mobile.presentation.component.ErrorRetryView
import com.gymvip.mobile.presentation.component.LoadingView
import com.gymvip.mobile.presentation.viewmodel.InboxViewModel
import com.gymvip.mobile.presentation.permission.DevicePermissionStatus
import com.gymvip.mobile.presentation.permission.rememberNotificationPermission
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import com.gymvip.mobile.presentation.theme.accentForeground

@Composable
fun InboxScreen(viewModel: InboxViewModel) {
    val state by viewModel.uiState.collectAsState()
    val notifications = rememberNotificationPermission()
    var unreadOnly by remember(viewModel) { mutableStateOf(false) }
    var selectedId by remember(viewModel) { mutableStateOf<String?>(null) }
    LaunchedEffect(state.openedItemId) { state.openedItemId?.let { selectedId = it } }
    val visibleItems = if (unreadOnly) state.items.filterNot { it.isRead } else state.items

    when {
        state.isLoading && state.items.isEmpty() -> LoadingView("Cargando avisos…")
        state.loadFailed && state.items.isEmpty() -> ErrorRetryView(
            message = state.loadErrorMessage ?: "No se pudieron cargar tus avisos.",
            onRetry = viewModel::refresh,
            modifier = Modifier.fillMaxSize()
        )
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ContextualHelpCard(
                    screenKey = "inbox_help",
                    title = "Bandeja de Avisos",
                    description = "Aquí recibirá comunicados oficiales, recordatorios de vencimiento y novedades de GYM VIP. Puede filtrar por avisos leídos o pendientes."
                )
            }
            if (notifications.status == DevicePermissionStatus.CAN_REQUEST || notifications.status == DevicePermissionStatus.DISABLED) {
                item {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Alertas del dispositivo desactivadas", fontWeight = FontWeight.SemiBold)
                            Text("Tus avisos siguen disponibles aquí. Activa las notificaciones si quieres recibir alertas en este teléfono.", style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = {
                                if (notifications.status == DevicePermissionStatus.CAN_REQUEST) notifications.request() else notifications.openSettings()
                            }) { Text("Activar notificaciones", color = MaterialTheme.colorScheme.accentForeground) }
                        }
                    }
                }
            }
            state.pushErrorMessage?.let { message ->
                item {
                    Text(message, color = MaterialTheme.colorScheme.onSurface)
                    TextButton(onClick = viewModel::refresh, enabled = !state.isLoading) { Text("Reintentar") }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = when (state.unreadCount) {
                            0 -> "Estás al día"
                            1 -> "1 aviso sin leer"
                            else -> "${state.unreadCount} avisos sin leer"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = viewModel::refresh, enabled = !state.isLoading) {
                        Text(if (state.isLoading) "Actualizando…" else "Actualizar avisos", color = MaterialTheme.colorScheme.accentForeground)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !unreadOnly,
                            onClick = { unreadOnly = false },
                            label = { Text("Todos") }
                        )
                        FilterChip(
                            selected = unreadOnly,
                            onClick = { unreadOnly = true },
                            label = { Text("Sin leer") }
                        )
                    }
                }
            }
            if (state.loadFailed) {
                item {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                state.loadErrorMessage ?: "Mostrando avisos guardados.",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall
                            )
                            TextButton(
                                onClick = viewModel::refresh,
                                enabled = !state.isLoading,
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.accentForeground)
                            ) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
            }
            if (visibleItems.isEmpty()) {
                item {
                    EmptyView(
                        title = if (unreadOnly) "Sin avisos sin leer" else "Todavía no hay avisos",
                        message = if (unreadOnly) "Puedes consultar los anteriores en Todos." else
                            "Los comunicados y recordatorios del gimnasio aparecerán aquí."
                    )
                }
            }
            items(visibleItems, key = { it.id }) { item ->
                InboxCard(item = item, onOpen = {
                    selectedId = item.id
                })
            }
        }
    }

    state.items.firstOrNull { it.id == selectedId }?.let { item ->
        InboxDetail(item, state.readingId == item.id, state.readErrorMessage, onMarkRead = { viewModel.markAsRead(item.id) }, onClose = { selectedId = null; viewModel.dismissPush() })
    }
}

@Composable
private fun InboxCard(item: InboxItem, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    OutlinedCard(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (item.isRead) colors.surface else colors.primaryContainer
        ),
        border = BorderStroke(1.dp, if (item.isRead) colors.outline else colors.primary.copy(alpha = 0.60f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            InboxCategoryIcon(item.category)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                InboxMetadata(item)
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (item.isRead) FontWeight.Medium else FontWeight.Bold
                )
                Text(
                    text = item.message,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!item.isRead) {
                    Text(
                        text = "Sin leer",
                        color = colors.accentForeground,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                modifier = Modifier.align(Alignment.CenterVertically).size(18.dp),
                tint = colors.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InboxDetail(item: InboxItem, reading: Boolean, readErrorMessage: String?, onMarkRead: () -> Unit, onClose: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            InboxCategoryIcon(item.category)
            InboxMetadata(item)
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = item.message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
            if (readErrorMessage != null && !item.isRead) {
                Text(readErrorMessage, color = MaterialTheme.colorScheme.error)
            }
            if (item.isRead) {
                Text("Leído", color = MaterialTheme.colorScheme.accentForeground)
            } else {
                Button(onClick = onMarkRead, enabled = !reading, modifier = Modifier.fillMaxWidth()) {
                    Text(if (reading) "Confirmando lectura…" else "Marcar como leído")
                }
            }
            TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
                Text("Volver a Avisos", color = MaterialTheme.colorScheme.accentForeground)
            }
        }
    }
}

@Composable
private fun InboxMetadata(item: InboxItem) {
    val date = remember(item.createdAt) { inboxDate(item.createdAt) }
    Text(
        text = listOfNotNull(item.category.label, date).joinToString(" · "),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun InboxCategoryIcon(category: InboxCategory) {
    val icon = when (category) {
        InboxCategory.RECORDATORIO -> Icons.Outlined.Event
        InboxCategory.VENCIMIENTO -> Icons.Outlined.Notifications
        InboxCategory.COMUNICADO -> Icons.Outlined.Campaign
        InboxCategory.PROMOCION -> Icons.Outlined.LocalOffer
    }
    Surface(
        modifier = Modifier.size(40.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.accentForeground)
        }
    }
}

private val InboxCategory.label: String
    get() = when (this) {
        InboxCategory.RECORDATORIO -> "Recordatorio"
        InboxCategory.VENCIMIENTO -> "Vencimiento"
        InboxCategory.COMUNICADO -> "Comunicado"
        InboxCategory.PROMOCION -> "Promoción"
    }

private fun inboxDate(value: String): String? {
    val isoValue = value.replace(' ', 'T')
    val instant = runCatching { Instant.parse(isoValue) }.getOrNull()
        ?: runCatching { LocalDateTime.parse(isoValue).toInstant(TimeZone.UTC) }.getOrNull()
    val date = instant?.toLocalDateTime(TimeZone.of("America/Lima"))?.date
        ?: runCatching { LocalDate.parse(value) }.getOrNull()
        ?: return null
    return "${date.dayOfMonth.toString().padStart(2, '0')}/${date.monthNumber.toString().padStart(2, '0')}/${date.year}"
}
