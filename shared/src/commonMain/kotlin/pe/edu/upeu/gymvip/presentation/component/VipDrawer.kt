package pe.edu.upeu.gymvip.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import pe.edu.upeu.gymvip.presentation.theme.GoldPrimary

@Composable
fun VipDrawerContent(
    memberName: String,
    memberEmail: String,
    onNavigateFaq: () -> Unit,
    onNavigateServices: () -> Unit,
    onNavigateTermsAndPrivacy: () -> Unit,
    onNavigatePreferences: () -> Unit,
    onSyncNow: () -> Unit,
    onLogout: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(20.dp)
        ) {
            // Header del socio
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GoldPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = memberName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.Black
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = memberName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = pe.edu.upeu.gymvip.domain.model.correoEnmascarado(memberEmail),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            // Ítems del menú secundario
            if (pe.edu.upeu.gymvip.AppConfig.FEATURE_SERVICIOS) DrawerMenuItem(
                icon = "🏋️",
                title = "Servicios e Instalaciones",
                subtitle = "Áreas y equipamiento VIP",
                onClick = onNavigateServices
            )

            DrawerMenuItem(
                icon = "❓",
                title = "Ayuda y Preguntas Frecuentes",
                subtitle = "Horarios, normas y soporte",
                onClick = onNavigateFaq
            )

            DrawerMenuItem(
                icon = "📜",
                title = "Términos y Privacidad",
                subtitle = "Consentimientos y datos",
                onClick = onNavigateTermsAndPrivacy
            )

            DrawerMenuItem(
                icon = "⚙️",
                title = "Preferencias de Tema",
                subtitle = "Modo Claro, Oscuro o Sistema",
                onClick = onNavigatePreferences
            )

            DrawerMenuItem(
                icon = "🔄",
                title = "Sincronizar Datos",
                subtitle = "Actualizar información del gimnasio",
                onClick = onSyncNow
            )

            Spacer(modifier = Modifier.weight(1f))
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            // Botón de Cerrar Sesión seguro
            DrawerMenuItem(
                icon = "🚪",
                title = "Cerrar Sesión",
                subtitle = "Eliminar credenciales locales",
                onClick = onLogout,
                isDanger = true
            )
        }
    }
}

@Composable
private fun DrawerMenuItem(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDanger: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
