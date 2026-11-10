package com.gymvip.mobile.presentation.screen.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.presentation.theme.GoldPrimary
import gymvip_mobile.composeapp.generated.resources.Res
import gymvip_mobile.composeapp.generated.resources.bg_athlete
import org.jetbrains.compose.resources.painterResource

@Composable
fun OnboardingScreen(
    onLogin: () -> Unit,
    onActivate: () -> Unit
) {
    var page by remember { mutableIntStateOf(0) }
    val titles = listOf("Tu membresía.\nSiempre clara.", "Entrena informado.\nDecide mejor.")
    val descriptions = listOf(
        "Consulta vigencia, pagos registrados y novedades sin trámites innecesarios.",
        "Revisa el catálogo, las promociones y los avisos oficiales de VIP Center Fit."
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Fondo deportista de espaldas con overlay oscuro de alto contraste
        Image(
            painter = painterResource(Res.drawable.bg_athlete),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xDF090B10),
                            Color(0xF0111722),
                            Color(0xFA090B10)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 18.dp)
        ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .border(1.dp, GoldPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("GV", color = GoldPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("GYM VIP", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onLogin) {
                    Text("Saltar", color = Color.White.copy(alpha = 0.82f))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = if (page == 0) "TU EXPERIENCIA, EN UN SOLO LUGAR" else "INFORMACIÓN CONFIABLE",
                color = GoldPrimary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = titles[page],
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = descriptions[page],
                color = Color(0xFFD1D5DB),
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(26.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(2) { index ->
                    Box(
                        modifier = Modifier
                            .width(if (index == page) 24.dp else 8.dp)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(if (index == page) GoldPrimary else Color(0xFF59606B))
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = { if (page == 0) page = 1 else onLogin() },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF17130A))
            ) {
                Text(if (page == 0) "Continuar" else "Iniciar sesión", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (page == 1) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onActivate,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(30.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A4250)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Activar mi acceso", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
}