package com.gymvip.mobile.presentation.screen.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.MotionPhotosOff
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymvip.mobile.presentation.screen.settings.SettingsPage
import com.gymvip.mobile.presentation.theme.accentForeground
import com.gymvip.mobile.presentation.viewmodel.PreferencesViewModel
import com.gymvip.mobile.util.isDevelopmentBuild

@Composable
fun PreferencesScreen(
    viewModel: PreferencesViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val accent = MaterialTheme.colorScheme.accentForeground

    SettingsPage(title = "Apariencia y Accesibilidad", onBack = onBack) {

        // ==========================================
        // 1. TEMA VISUAL
        // ==========================================
        Text(
            text = "TEMA VISUAL",
            style = MaterialTheme.typography.labelLarge,
            color = accent,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                ThemeOptionRow(
                    label = "Seguir tema del sistema",
                    subtitle = "Alterna automáticamente entre claro y oscuro según Android",
                    selected = state.currentTheme == "SYSTEM",
                    onClick = { viewModel.setTheme("SYSTEM") }
                )
                ThemeOptionRow(
                    label = "Azul Deportivo Oficial (Oscuro)",
                    subtitle = "Identidad GYM VIP con fondo azul profundo #0B1320",
                    selected = state.currentTheme == "DARK",
                    onClick = { viewModel.setTheme("DARK") }
                )
                ThemeOptionRow(
                    label = "Azul Deportivo Oficial (Claro)",
                    subtitle = "Fondo blanco nítido con acentos azul marino",
                    selected = state.currentTheme == "LIGHT",
                    onClick = { viewModel.setTheme("LIGHT") }
                )
                ThemeOptionRow(
                    label = "Dorado Luxury (Alternativa Dark & Gold)",
                    subtitle = "Negro carbón con acentos dorados de alto contraste",
                    selected = state.currentTheme == "DARK_GOLD",
                    onClick = { viewModel.setTheme("DARK_GOLD") }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // 2. ACCESIBILIDAD Y LECTURA
        // ==========================================
        Text(
            text = "ACCESIBILIDAD Y LECTURA",
            style = MaterialTheme.typography.labelLarge,
            color = accent,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Escala de tamaño de texto
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.FormatSize, contentDescription = null, tint = accent)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tamaño de texto", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Ajusta el tamaño respetando la configuración del sistema",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScaleOptionChip(
                        label = "100%",
                        selected = state.fontScale <= 1.05f,
                        onClick = { viewModel.setFontScale(1.0f) },
                        modifier = Modifier.weight(1f)
                    )
                    ScaleOptionChip(
                        label = "115%",
                        selected = state.fontScale in 1.06f..1.22f,
                        onClick = { viewModel.setFontScale(1.15f) },
                        modifier = Modifier.weight(1f)
                    )
                    ScaleOptionChip(
                        label = "130%",
                        selected = state.fontScale > 1.22f,
                        onClick = { viewModel.setFontScale(1.30f) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Alto contraste
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Contrast, contentDescription = null, tint = accent)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mayor contraste", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Refuerza bordes y contraste tipográfico para TalkBack y baja visión",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.highContrast,
                        onCheckedChange = { viewModel.setHighContrast(it) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reducción de animaciones
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.MotionPhotosOff, contentDescription = null, tint = accent)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reducción de animaciones", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Reduce transiciones y movimientos para evitar fatiga visual",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.reduceMotion,
                        onCheckedChange = { viewModel.setReduceMotion(it) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // 3. VISTA PREVIA INTERACTIVA EN TIEMPO REAL
        // ==========================================
        Text(
            text = "VISTA PREVIA",
            style = MaterialTheme.typography.labelLarge,
            color = accent,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Membresía VIP Center Fit",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "30 días restantes • Vence: 20/12/2026",
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Este texto refleja el tamaño de fuente, contraste y tema visual seleccionados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón Restaurar Valores Predeterminados
        OutlinedButton(
            onClick = { viewModel.resetToDefaults() },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
            Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Restaurar valores predeterminados")
        }

        // ==========================================
        // 4. SERVIDOR API (SOLO DESARROLLO)
        // ==========================================
        if (isDevelopmentBuild) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "ENTORNO / SERVIDOR API (Desarrollo)",
                style = MaterialTheme.typography.labelLarge,
                color = accent
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    ThemeOptionRow(
                        label = "Servidor Render (Nube Oficial)",
                        subtitle = "https://gymvip-backend.onrender.com",
                        selected = state.currentServer.contains("onrender.com"),
                        onClick = { viewModel.setServer("https://gymvip-backend.onrender.com") }
                    )
                    ThemeOptionRow(
                        label = "Emulador Android (Local)",
                        subtitle = "http://10.0.2.2:8081",
                        selected = state.currentServer.contains("10.0.2.2"),
                        onClick = { viewModel.setServer("http://10.0.2.2:8081") }
                    )
                    ThemeOptionRow(
                        label = "Host Local / Red Local",
                        subtitle = "http://localhost:8081",
                        selected = state.currentServer.contains("localhost"),
                        onClick = { viewModel.setServer("http://localhost:8081") }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScaleOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = contentColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ThemeOptionRow(
    label: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.accentForeground)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
