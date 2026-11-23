package com.gymvip.mobile.presentation.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.di.AppContainer
import kotlinx.coroutines.launch

@Composable
internal fun SettingsPage(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().background(colors.background).safeDrawingPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver", tint = colors.onBackground)
            }
            Text(title, style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
        }
        HorizontalDivider(color = colors.outlineVariant)
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
internal fun SettingsScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    canEditProfile: Boolean = true,
    onAccountSecurity: () -> Unit,
    onPermissions: () -> Unit,
    onAppearance: () -> Unit,
    onTermsAndPrivacy: () -> Unit,
    onLogout: () -> Unit
) {
    SettingsPage(title = "Ajustes", onBack = onBack) {
        SettingsSection("Perfil y cuenta") {
            if (canEditProfile) {
                SettingsRow("Editar perfil", Icons.Outlined.Badge, onEditProfile)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            SettingsRow("Cuenta y seguridad", Icons.Outlined.ManageAccounts, onAccountSecurity)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingsRow("Permisos de la aplicación", Icons.Outlined.Security, onPermissions)
        }
        SettingsSection("Apariencia") {
            SettingsRow("Tema de la aplicación", Icons.Outlined.Palette, onAppearance)
        }
        val scope = rememberCoroutineScope()
        var resetHelpFeedback by remember { mutableStateOf<String?>(null) }
        SettingsSection("Ayuda y tutoriales") {
            SettingsRow("Restablecer guías y ayudas contextuales", Icons.AutoMirrored.Outlined.HelpOutline) {
                scope.launch {
                    listOf(
                        "login_help",
                        "activation_help",
                        "membership_help",
                        "home_checkin_help",
                        "catalog_help",
                        "inbox_help",
                        "onboarding_completed",
                        "permissions_explainer_completed"
                    ).forEach { key ->
                        AppContainer.configRepository.setBooleanPreference(key, false)
                    }
                    resetHelpFeedback = "Guías y ayudas restablecidas. Se volverán a mostrar al ingresar a cada sección."
                }
            }
            if (resetHelpFeedback != null) {
                Text(
                    text = resetHelpFeedback.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        SettingsSection("Legal") {
            SettingsRow("Términos y privacidad", Icons.Outlined.Description, onTermsAndPrivacy)
        }
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
            Text("Cerrar sesión", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
internal fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(content = content)
        }
    }
}

@Composable
internal fun SettingsRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null)
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
        }
    }
}
