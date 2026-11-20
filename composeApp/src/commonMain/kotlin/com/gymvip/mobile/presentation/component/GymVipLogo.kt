package com.gymvip.mobile.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Componente unificado de identidad visual GYM VIP.
 * Renderiza el monograma oficial "GV" con aros concéntricos e insignia de prestigio.
 */
@Composable
fun GymVipLogo(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    useGoldVariant: Boolean = false,
    primaryColor: Color = if (useGoldVariant) Color(0xFFD4AF37) else Color(0xFF008DDA),
    secondaryColor: Color = if (useGoldVariant) Color(0xFFF59E0B) else Color(0xFFFFFFFF),
    accentColor: Color = Color(0xFFD4AF37)
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension / 2f

            // Aro Exterior
            drawCircle(
                color = primaryColor,
                radius = baseRadius * 0.90f,
                center = center,
                style = Stroke(width = baseRadius * 0.075f)
            )

            // Aro Interior Fino
            drawCircle(
                color = accentColor.copy(alpha = 0.8f),
                radius = baseRadius * 0.81f,
                center = center,
                style = Stroke(width = baseRadius * 0.035f)
            )

            // Letra G simétrica de alto contraste
            val gPath = Path().apply {
                val scale = baseRadius / 54f
                moveTo(center.x - 7f * scale, center.y - 11f * scale)
                cubicTo(
                    center.x - 11f * scale, center.y - 14f * scale,
                    center.x - 16f * scale, center.y - 14f * scale,
                    center.x - 19.5f * scale, center.y - 10.5f * scale
                )
                cubicTo(
                    center.x - 24f * scale, center.y - 6f * scale,
                    center.x - 24f * scale, center.y + 8f * scale,
                    center.x - 19.5f * scale, center.y + 12.5f * scale
                )
                cubicTo(
                    center.x - 15.5f * scale, center.y + 16f * scale,
                    center.x - 10f * scale, center.y + 15f * scale,
                    center.x - 6.5f * scale, center.y + 11.5f * scale
                )
                lineTo(center.x - 6.5f * scale, center.y + 1f * scale)
                lineTo(center.x - 14f * scale, center.y + 1f * scale)
            }
            drawPath(
                path = gPath,
                color = secondaryColor,
                style = Stroke(
                    width = baseRadius * 0.12f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Letra V simétrica de alto contraste
            val vPath = Path().apply {
                val scale = baseRadius / 54f
                moveTo(center.x + 0.5f * scale, center.y - 11.5f * scale)
                lineTo(center.x + 9.5f * scale, center.y + 12.5f * scale)
                lineTo(center.x + 18.5f * scale, center.y - 11.5f * scale)
            }
            drawPath(
                path = vPath,
                color = primaryColor,
                style = Stroke(
                    width = baseRadius * 0.12f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Estrella de prestigio inferior
            drawStar(
                center = Offset(center.x, center.y + baseRadius * 0.58f),
                radius = baseRadius * 0.11f,
                color = accentColor
            )
        }
    }
}

private fun DrawScope.drawStar(center: Offset, radius: Float, color: Color) {
    val path = Path()
    val points = 5
    val innerRadius = radius * 0.45f
    val doublePi = Math.PI * 2
    val step = Math.PI / points

    var angle = -Math.PI / 2
    path.moveTo(
        (center.x + radius * Math.cos(angle)).toFloat(),
        (center.y + radius * Math.sin(angle)).toFloat()
    )

    for (i in 0 until points) {
        angle += step
        path.lineTo(
            (center.x + innerRadius * Math.cos(angle)).toFloat(),
            (center.y + innerRadius * Math.sin(angle)).toFloat()
        )
        angle += step
        path.lineTo(
            (center.x + radius * Math.cos(angle)).toFloat(),
            (center.y + radius * Math.sin(angle)).toFloat()
        )
    }
    path.close()
    drawPath(path = path, color = color)
}
