package com.gymvip.mobile.presentation.screen.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymvip.mobile.presentation.component.GymVipLogo
import com.gymvip.mobile.presentation.theme.LocalGymVipSystemTheme

@Composable
fun SplashScreen(
    onResolved: (Boolean) -> Unit,
    onRetry: (() -> Unit)? = null,
    isSessionActive: Boolean,
    isResolutionFailed: Boolean = false,
    errorMessage: String? = null
) {
    val followsSystem = LocalGymVipSystemTheme.current
    val accent = if (followsSystem) Color(0xFF008DDA) else Color(0xFFD4AF37)

    // Sin esperas artificiales: en cuanto el estado de sesión esté resuelto y no haya fallo, transicionar
    LaunchedEffect(isSessionActive, isResolutionFailed) {
        if (!isResolutionFailed) {
            onResolved(isSessionActive)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B1320),
                        Color(0xFF111C2E),
                        Color(0xFF0B1320)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Logo Unificado GV nítido
            GymVipLogo(
                size = 110.dp,
                useGoldVariant = !followsSystem
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "GYM VIP",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Text(
                text = "VIP CENTER FIT",
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Experiencia Digital del Socio",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(40.dp))

            if (!isResolutionFailed) {
                CircularProgressIndicator(
                    color = accent,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
            } else {
                // Si la restauración de sesión falló por conectividad o servidor,
                // permitir continuar o reintentar sin esperas artificiales
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "No fue posible sincronizar la sesión con el servidor.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFCA5A5),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onRetry?.invoke() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text("Reintentar", style = MaterialTheme.typography.labelLarge)
                        }

                        Button(
                            onClick = { onResolved(false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text("Continuar", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
