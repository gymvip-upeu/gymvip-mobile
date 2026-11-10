package pe.edu.upeu.gymvip.domain

import androidx.compose.ui.graphics.luminance
import kotlinx.datetime.Instant
import pe.edu.upeu.gymvip.domain.model.MembershipStatus
import pe.edu.upeu.gymvip.domain.model.fechaAvisoSocio
import pe.edu.upeu.gymvip.presentation.theme.coloresEstado
import kotlin.test.*

class PulidoVisualTest {
    @Test fun etiquetasDeEstadoCumplenContrasteDeTextoPequenoEnAmbosTemas() {
        for (oscuro in listOf(true, false)) for (estado in MembershipStatus.entries) {
            val (fondo, texto) = coloresEstado(estado, oscuro)
            val claro = maxOf(fondo.luminance(), texto.luminance())
            val oscuroL = minOf(fondo.luminance(), texto.luminance())
            assertTrue((claro + .05f) / (oscuroL + .05f) >= 4.5f, "Contraste insuficiente: $estado, tema oscuro=$oscuro")
        }
    }
    @Test fun hoyYAyerUsanElDiaDeLimaInclusoEnMedianocheUtc() {
        val ahora = Instant.parse("2026-11-10T04:59:00Z")
        assertEquals("Hoy · 20:00", fechaAvisoSocio("2026-11-10T01:00:00", ahora))
        assertEquals("Ayer · 23:59", fechaAvisoSocio("2026-11-09T04:59:00Z", ahora))
        assertEquals("Hoy · 00:00", fechaAvisoSocio("2026-11-09T05:00:00Z", ahora))
    }
    @Test fun fechasAnterioresNoPierdenAnoNiOcultanErrores() {
        val ahora = Instant.parse("2026-11-09T17:00:00Z")
        assertEquals("8 nov 2025 · 12:00", fechaAvisoSocio("2025-11-08T17:00:00Z", ahora))
        assertEquals("Fecha no disponible", fechaAvisoSocio("invalido", ahora))
    }
}
