package com.gymvip.mobile.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class AvatarOption(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val gradientColors: List<Color>
)

val LocalAvatars = listOf(
    AvatarOption("atleta_vip", "Atleta VIP", Icons.Outlined.FitnessCenter, listOf(Color(0xFF008DDA), Color(0xFF1E3E62))),
    AvatarOption("halcon_rayo", "Rayo Fuerza", Icons.Outlined.FlashOn, listOf(Color(0xFFF59E0B), Color(0xFFB45309))),
    AvatarOption("corona_gold", "Corona Oro", Icons.Outlined.WorkspacePremium, listOf(Color(0xFFD4AF37), Color(0xFF854D0E))),
    AvatarOption("campeon_trofeo", "Campeón", Icons.Outlined.EmojiEvents, listOf(Color(0xFF10B981), Color(0xFF047857))),
    AvatarOption("fuego_activo", "Energía Fit", Icons.Outlined.LocalFireDepartment, listOf(Color(0xFFEF4444), Color(0xFF991B1B))),
    AvatarOption("velocidad_run", "Velocidad", Icons.Outlined.Speed, listOf(Color(0xFF38BDF8), Color(0xFF0369A1))),
    AvatarOption("medalla_honor", "Medalla Honor", Icons.Outlined.MilitaryTech, listOf(Color(0xFFA855F7), Color(0xFF6B21A8))),
    AvatarOption("escudo_power", "Escudo Power", Icons.Outlined.Shield, listOf(Color(0xFF64748B), Color(0xFF1E293B)))
)

/**
 * Renderiza un avatar local creativo basado en una semilla fija por cuenta.
 * No realiza peticiones de red ni envía PII a servicios externos.
 */
@Composable
fun UserAvatar(
    seed: String,
    size: Dp = 64.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val avatar = LocalAvatars.firstOrNull { it.id == seed } ?: resolveAvatarFromSeed(seed)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(avatar.gradientColors))
            .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = avatar.icon,
            contentDescription = avatar.name,
            tint = Color.White,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}

private fun resolveAvatarFromSeed(seed: String): AvatarOption {
    if (seed.isBlank()) return LocalAvatars[0]
    val hash = Math.abs(seed.hashCode())
    val index = hash % LocalAvatars.size
    return LocalAvatars[index]
}

@Composable
fun AvatarSelectionDialog(
    currentSeed: String,
    onSelectAvatar: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Seleccionar Avatar de Cuenta",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Elige tu avatar representativo para la aplicación. Tu elección se guarda localmente en tu dispositivo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                ) {
                    items(LocalAvatars) { avatar ->
                        val isSelected = avatar.id == currentSeed
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(avatar.gradientColors))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color(0xFFD4AF37) else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onSelectAvatar(avatar.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = avatar.icon,
                                contentDescription = avatar.name,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val randomAvatar = LocalAvatars.random()
                    onSelectAvatar(randomAvatar.id)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Outlined.Casino, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Aleatorio")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("Cerrar")
            }
        }
    )
}
