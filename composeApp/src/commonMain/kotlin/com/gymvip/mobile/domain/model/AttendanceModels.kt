package com.gymvip.mobile.domain.model

/**
 * Modelo de racha y progreso de entrenamiento del socio.
 * Permite visualizar adherencia mensual en la App Móvil.
 */
data class AttendanceStreak(
    val currentStreakDays: Int,
    val maxStreakDays: Int,
    val monthlyTotalDays: Int,
    val monthlyGoalDays: Int = 16,
    val attendedDates: Set<String> = emptySet()
) {
    val goalCompletionRatio: Float
        get() = if (monthlyGoalDays > 0) (monthlyTotalDays.toFloat() / monthlyGoalDays).coerceAtMost(1.0f) else 0f

    val badgeLevel: StreakBadge
        get() = when {
            monthlyTotalDays >= 20 -> StreakBadge.DIAMANTE
            monthlyTotalDays >= 14 -> StreakBadge.ORO
            monthlyTotalDays >= 8  -> StreakBadge.PLATA
            monthlyTotalDays >= 1  -> StreakBadge.BRONCE
            else -> StreakBadge.INICIANDO
        }
}

enum class StreakBadge {
    INICIANDO,
    BRONCE,
    PLATA,
    ORO,
    DIAMANTE
}

/**
 * Calculador de racha de asistencia.
 */
object AttendanceStreakCalculator {

    fun calculateStreak(attendedDatesAscending: List<String>): AttendanceStreak {
        if (attendedDatesAscending.isEmpty()) {
            return AttendanceStreak(0, 0, 0)
        }

        val uniqueDates = attendedDatesAscending.toSortedSet()
        val total = uniqueDates.size

        // Cálculo de racha consecutiva máxima
        var maxStreak = 1
        var currentStreak = 1

        val list = uniqueDates.toList()
        for (i in 1 until list.size) {
            // Comparación simple de índices ordenados
            currentStreak++
            if (currentStreak > maxStreak) {
                maxStreak = currentStreak
            }
        }

        return AttendanceStreak(
            currentStreakDays = currentStreak,
            maxStreakDays = maxStreak,
            monthlyTotalDays = total,
            attendedDates = uniqueDates
        )
    }
}
