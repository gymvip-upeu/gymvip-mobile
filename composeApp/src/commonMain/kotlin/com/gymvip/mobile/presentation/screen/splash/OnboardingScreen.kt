package com.gymvip.mobile.presentation.screen.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymvip.mobile.presentation.component.GymVipLogo
import com.gymvip.mobile.presentation.theme.LocalGymVipSystemTheme
import gymvip_mobile.composeapp.generated.resources.Res
import gymvip_mobile.composeapp.generated.resources.onboarding_qr
import gymvip_mobile.composeapp.generated.resources.onboarding_supplements
import gymvip_mobile.composeapp.generated.resources.onboarding_welcome
import gymvip_mobile.composeapp.generated.resources.onboarding_community
import org.jetbrains.compose.resources.painterResource

data class OnboardingPageData(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector
)

@Composable
fun OnboardingScreen(
    onLogin: () -> Unit = {},
    onActivate: () -> Unit = {},
    onFinish: () -> Unit = onLogin,
    onLoginDirect: () -> Unit = onLogin,
    onActivateDirect: () -> Unit = onActivate
) {
    val followsSystem = LocalGymVipSystemTheme.current
    val accent = if (followsSystem) Color(0xFF008DDA) else Color(0xFFD4AF37)

    val pages = remember {
        listOf(
            OnboardingPageData(
                title = "Bienvenido a GYM VIP",
                subtitle = "TU CLUB DIGITAL",
                description = "Gestiona tu membresía, días de vigencia y pagos registrados en el club con total transparencia y rapidez.",
                icon = Icons.Outlined.FitnessCenter
            ),
            OnboardingPageData(
                title = "Acceso Rápido con QR",
                subtitle = "CONTROL DE ASISTENCIA",
                description = "Ingresa al gimnasio escaneando el código QR en la pantalla de recepción directamente desde tu cámara.",
                icon = Icons.Outlined.QrCodeScanner
            ),
            OnboardingPageData(
                title = "Catálogo y Suplementos",
                subtitle = "NUTRICIÓN Y EQUIPAMIENTO",
                description = "Explora suplementos deportivos oficiales, consulta stock, precios vigentes y guarda tus productos favoritos.",
                icon = Icons.Outlined.ShoppingBag
            ),
            OnboardingPageData(
                title = "Avisos y Comunidad",
                subtitle = "SIEMPRE INFORMADO",
                description = "Recibe notificaciones de renovación, comunicados oficiales de la sede y resuelve tus dudas en la sección de preguntas frecuentes.",
                icon = Icons.Outlined.NotificationsActive
            )
        )
    }

    var currentPage by remember { mutableIntStateOf(0) }
    var dragDistance by remember { mutableFloatStateOf(0f) }
    val isLastPage = currentPage == pages.size - 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(pages.size) {
                detectHorizontalDragGestures(
                    onDragStart = { dragDistance = 0f },
                    onDragEnd = {
                        if (dragDistance < -80f && currentPage < pages.lastIndex) currentPage++
                        if (dragDistance > 80f && currentPage > 0) currentPage--
                        dragDistance = 0f
                    },
                    onHorizontalDrag = { _, amount -> dragDistance += amount }
                )
            }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B1320),
                        Color(0xFF142033),
                        Color(0xFF0B1320)
                    )
                )
            )
            .systemBarsPadding()
    ) {
        val background = when (currentPage) {
            0 -> Res.drawable.onboarding_welcome
            1 -> Res.drawable.onboarding_qr
            2 -> Res.drawable.onboarding_supplements
            else -> Res.drawable.onboarding_community
        }
        Image(
            painter = painterResource(background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.0f to Color(0xB30B1320),
                    0.35f to Color(0x730B1320),
                    0.70f to Color(0x990B1320),
                    1.0f to Color(0xD90B1320)
                )
            )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Barra Superior: Logo GV y Botón Saltar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GymVipLogo(size = 38.dp, useGoldVariant = !followsSystem)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GYM VIP",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                if (!isLastPage) {
                    TextButton(onClick = onFinish) {
                        Text(
                            text = "Saltar",
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.15f))

            // Contenido de la Pantalla Actual
            val current = pages[currentPage]

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = current.icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = current.subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = current.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = current.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.weight(0.2f))

            // Indicador de Puntos (Dots Indicator interactivo)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.indices.forEach { index ->
                    val isSelected = index == currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(width = if (isSelected) 24.dp else 8.dp, height = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) accent else Color(0xFF475569))
                            .clickable { currentPage = index }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Botones de Navegación Accesibles (Funcionan con y sin gestos)
            if (!isLastPage) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentPage > 0) {
                        OutlinedButton(
                            onClick = { currentPage -= 1 },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Atrás")
                        }
                    }

                    Button(
                        onClick = { currentPage += 1 },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(if (currentPage > 0) 1f else 2f)
                            .height(48.dp)
                    ) {
                        Text("Continuar", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            } else {
                // Última página: Opciones directas de entrada
                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onLoginDirect,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Iniciar Sesión", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onActivateDirect,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Activar Cuenta de Socio", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
