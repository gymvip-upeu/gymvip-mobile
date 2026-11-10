package pe.edu.upeu.gymvip.presentation.theme

import androidx.compose.ui.graphics.Color

// Dark & Gold Luxury Tokens oficiales
val DarkBackground = Color(0xFF0B0D12)
val DarkSurfacePanel = Color(0xFF151922)
val DarkSurfaceElevated = Color(0xFF1D2330)
val DarkSurfaceInput = Color(0xFF11141B)
val DarkBorder = Color(0xFF232834)

val GoldPrimary = Color(0xFFE5C158)
val GoldHover = Color(0xFFD4AF37)
val GoldOnPrimary = Color(0xFF14110A)
val GoldSubtle = Color(0x1FE5C158)

val TextPrimaryDark = Color(0xFFF5F6F8)
val TextSecondaryDark = Color(0xFF9AA3B2)
val TextMutedDark = Color(0xFF7E8B9B)

// Estados semánticos de alto contraste
val SuccessGreen = Color(0xFF22C55E)
val WarningAmber = Color(0xFFF59E0B)
val DangerRed = Color(0xFFEF4444)
val NeutralSlate = Color(0xFF94A3B8)

// Light Theme Tokens
val LightBackground = Color(0xFFF7F6F2)
val LightSurfacePanel = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFEFEFEA)
val LightPrimary = Color(0xFF735711)
val LightOnPrimary = Color(0xFFFFFFFF)
val TextPrimaryLight = Color(0xFF1D1C19)
val TextSecondaryLight = Color(0xFF555450)
val LightBorder = Color(0xFFD6D5CD)

/** Fondo y texto opacos para que los estados pequeños mantengan contraste en ambos temas. */
fun coloresEstado(estado: pe.edu.upeu.gymvip.domain.model.MembershipStatus, oscuro: Boolean): Pair<Color, Color> {
    val tipo = when (estado) {
        pe.edu.upeu.gymvip.domain.model.MembershipStatus.VIGENTE -> 0
        pe.edu.upeu.gymvip.domain.model.MembershipStatus.VENCIDA,
        pe.edu.upeu.gymvip.domain.model.MembershipStatus.CANCELADA -> 1
        pe.edu.upeu.gymvip.domain.model.MembershipStatus.SIN_MEMBRESIA -> 2
        else -> 3
    }
    return when (tipo) {
        0 -> if (oscuro) Color(0xFF173221) to Color(0xFF86EFAC) else Color(0xFFDCFCE7) to Color(0xFF166534)
        1 -> if (oscuro) Color(0xFF3C191D) to Color(0xFFFCA5A5) else Color(0xFFFEE2E2) to Color(0xFF991B1B)
        2 -> if (oscuro) DarkSurfaceElevated to TextSecondaryDark else LightSurfaceElevated to TextSecondaryLight
        else -> if (oscuro) Color(0xFF33260F) to Color(0xFFFCD34D) else Color(0xFFFEF3C7) to Color(0xFF92400E)
    }
}
