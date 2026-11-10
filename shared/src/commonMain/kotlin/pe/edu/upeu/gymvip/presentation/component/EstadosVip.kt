package pe.edu.upeu.gymvip.presentation.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun Esqueleto(modifier: Modifier = Modifier) {
    val transicion = rememberInfiniteTransition(label = "Carga de información")
    val alpha by transicion.animateFloat(0.3f, 0.7f,
        infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "Intensidad de carga")
    Column(modifier.fillMaxWidth().semantics { contentDescription = "Cargando información" },
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(3) { indice ->
            Spacer(Modifier.fillMaxWidth(if (indice == 0) 0.6f else 1f)
                .height(if (indice == 0) 24.dp else 64.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha), RoundedCornerShape(16.dp)))
        }
    }
}

@Composable
fun EstadoVacio(mensaje: String, modifier: Modifier = Modifier) {
    val borde = MaterialTheme.colorScheme.outline
    Box(modifier.fillMaxWidth()) {
        Canvas(Modifier.matchParentSize()) {
            drawRoundRect(borde, cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),
                style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))))
        }
        Text(mensaje, Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EstadoError(mensaje: String, reintentar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(mensaje, color = MaterialTheme.colorScheme.error)
        BotonSecundario("Reintentar", reintentar)
    }
}

@Composable
fun ChipFiltro(texto: String, seleccionado: Boolean, seleccionar: () -> Unit) {
    FilterChip(seleccionado, seleccionar, label = { Text(texto) }, modifier = Modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(20.dp))
}
