package com.gymvip.mobile.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.presentation.theme.DangerRed
import com.gymvip.mobile.presentation.theme.WarningAmber

enum class ConnectionNoticeType {
    NONE,
    OFFLINE_NETWORK,
    SERVER_ERROR,
    SESSION_EXPIRED
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
        ConnectionNoticeType.SESSION_EXPIRED -> "⚠️"
        ConnectionNoticeType.SERVER_ERROR -> "⚡"
        ConnectionNoticeType.OFFLINE_NETWORK -> "📡"
        ConnectionNoticeType.NONE -> "🔄"
    }

    val color = when (effectiveType) {
        ConnectionNoticeType.SESSION_EXPIRED -> DangerRed
        else -> WarningAmber
    }

    val defaultMessage = when (effectiveType) {
        ConnectionNoticeType.SESSION_EXPIRED -> "Sesión expirada. Por favor, vuelva a iniciar sesión para sincronizar sus datos."
        ConnectionNoticeType.SERVER_ERROR -> "El servidor de GYM VIP no responde temporalmente. Mostrando datos locales de su última sesión."
        ConnectionNoticeType.OFFLINE_NETWORK -> {
            if (pendingSyncCount > 0) {
                "Modo sin conexión. $pendingSyncCount cambio(s) guardados localmente para sincronizar al reconectar."
            } else {
                "Modo sin conexión. Mostrando datos locales de su última sesión."
            }
        }
        ConnectionNoticeType.NONE -> {
            "$pendingSyncCount cambio(s) pendientes de sincronización con el servidor."
        }
    }

    val message = customMessage ?: defaultMessage

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}
