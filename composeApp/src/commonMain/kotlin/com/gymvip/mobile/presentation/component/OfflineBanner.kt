package com.gymvip.mobile.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class ConnectionNoticeType {
    NONE,
    OFFLINE_NETWORK,
    SERVER_ERROR,
    SESSION_EXPIRED,
    ACCESS_DENIED
}

@Composable
fun OfflineBanner(
    isOffline: Boolean,
    noticeType: ConnectionNoticeType = ConnectionNoticeType.NONE,
    customMessage: String? = null,
    pendingSyncCount: Int = 0,
    modifier: Modifier = Modifier
) {
    if (!isOffline && noticeType == ConnectionNoticeType.NONE && pendingSyncCount == 0) return

    val effectiveType = if (noticeType != ConnectionNoticeType.NONE) {
        noticeType
    } else if (isOffline) {
        ConnectionNoticeType.OFFLINE_NETWORK
    } else {
        ConnectionNoticeType.NONE
    }

    val icon = when (effectiveType) {
        ConnectionNoticeType.SESSION_EXPIRED -> Icons.Outlined.ErrorOutline
        ConnectionNoticeType.ACCESS_DENIED -> Icons.Outlined.ErrorOutline
        ConnectionNoticeType.SERVER_ERROR -> Icons.Outlined.CloudOff
        ConnectionNoticeType.OFFLINE_NETWORK -> Icons.Outlined.WifiOff
        ConnectionNoticeType.NONE -> Icons.Outlined.Sync
    }

    val color = when (effectiveType) {
        ConnectionNoticeType.SESSION_EXPIRED, ConnectionNoticeType.ACCESS_DENIED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    val background = if (effectiveType == ConnectionNoticeType.SESSION_EXPIRED || effectiveType == ConnectionNoticeType.ACCESS_DENIED) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    val defaultMessage = when (effectiveType) {
        ConnectionNoticeType.SESSION_EXPIRED -> "Sesión expirada. Por favor, vuelva a iniciar sesión para sincronizar sus datos."
        ConnectionNoticeType.ACCESS_DENIED -> "Esta cuenta no tiene permiso para consultar estos datos."
        ConnectionNoticeType.SERVER_ERROR -> "El servidor de GYM VIP no responde temporalmente. Mostrando datos locales de su última sesión."
        ConnectionNoticeType.OFFLINE_NETWORK -> {
            if (pendingSyncCount > 0) {
                "Modo sin conexión. $pendingSyncCount cambio(s) guardados localmente para sincronizar al reconectar."
            } else {
                "Modo sin conexión. Mostrando datos locales de su última sesión."
            }
        }
        ConnectionNoticeType.NONE -> {
            if (pendingSyncCount == 1) "1 cambio pendiente de sincronización." else
                "$pendingSyncCount cambios pendientes de sincronización."
        }
    }

    val message = customMessage ?: defaultMessage

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}
