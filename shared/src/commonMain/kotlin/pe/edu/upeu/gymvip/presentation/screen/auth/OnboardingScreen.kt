package pe.edu.upeu.gymvip.presentation.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.edu.upeu.gymvip.presentation.component.*
import org.jetbrains.compose.resources.painterResource
import gymvip.shared.generated.resources.Res
import gymvip.shared.generated.resources.bienvenida_gym

@Composable
fun OnboardingScreen(login: () -> Unit, activar: () -> Unit) {
    val pager = rememberPagerState(pageCount = { 2 })
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box {
            val fondo = MaterialTheme.colorScheme.background
            Image(painterResource(Res.drawable.bienvenida_gym), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(
                fondo.copy(alpha = if (fondo.luminance() < .5f) .15f else .8f),
                fondo.copy(alpha = .45f), fondo.copy(alpha = .97f), fondo))))
            BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            val altura = maxHeight
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = altura).padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LogoGV(); Text("GYM VIP", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(login) { Text("Saltar") }
                }
                Column(Modifier.padding(top = 40.dp)) {
                HorizontalPager(pager, modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp)) { pagina ->
                    Column(Modifier.padding(vertical = 24.dp)) {
                        Text(if (pagina == 0) "TU ESPACIO VIP" else "INFORMACIÓN CONFIABLE",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.2.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(if (pagina == 0) "Tu membresía," else "Entrena informado.", fontSize = 32.sp, lineHeight = 36.sp)
                        Text(if (pagina == 0) "siempre a la mano." else "Decide mejor.", fontSize = 32.sp, lineHeight = 36.sp,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text(if (pagina == 0) "Consulta tu vigencia, pagos y novedades de VIP Center Fit desde un solo lugar."
                            else "Revisa el catálogo, promociones y avisos oficiales de VIP Center Fit.",
                            style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(2) { i -> Spacer(Modifier.size(if (i == pager.currentPage) 22.dp else 6.dp, 6.dp)
                        .background(if (i == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(8.dp))) }
                }
                Spacer(Modifier.height(20.dp))
                BotonPrimario("Iniciar sesión", login)
                Spacer(Modifier.height(12.dp))
                BotonSecundario("Activar mi acceso", activar)
                }
            }
            }
        }
    }
}
