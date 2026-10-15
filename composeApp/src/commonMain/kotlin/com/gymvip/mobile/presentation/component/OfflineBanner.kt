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
import com.gymvip.mobile.presentation.theme.WarningAmber

@Composable
fun OfflineBanner(
    isOffline: Boolean,
    pendingSyncCount: Int = 0,
    modifier: Modifier = Modifier
) {
    if (!isOffline && pendingSyncCount == 0) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(WarningAmber.copy(alpha = 0.18f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "📡", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.width(8.dp))
            val message = if (isOffline) {
                if (pendingSyncCount > 0) {
                    "Modo sin conexión. $pendingSyncCount cambio(s) guardados localmente para sincronizar al reconectar."
                } else {
                    "Modo sin conexión. Mostrando datos locales de su última sesión."
                }
            } else {
                "$pendingSyncCount cambio(s) pendientes de sincronización con el servidor."
            }
            Text(
                text = message,
                style = MaterialTheme.typography.labelSmall,
                color = WarningAmber
            )
        }
    }
}
