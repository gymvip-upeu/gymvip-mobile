package com.gymvip.mobile.presentation.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.presentation.viewmodel.ProfileViewModel

@Composable
internal fun AccountSecurityScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onChangePassword: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    SettingsPage(title = "Cuenta y seguridad", onBack = onBack) {
        SettingsSection("Credenciales") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Outlined.Email, contentDescription = null)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Correo electrónico", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = state.profile?.email?.takeIf { it.isNotBlank() }
                            ?: if (state.isLoading) "Cargando correo…" else "Correo no disponible",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingsRow("Cambiar contraseña", Icons.Outlined.Lock, onChangePassword)
        }
    }
}
