package com.gymvip.mobile.features.journal

import kotlinx.datetime.*
import kotlinx.serialization.Serializable

@Serializable
data class JournalExercise(val name: String, val sets: Int, val reps: Int, val kg: Double)

@Serializable
data class JournalEntry(
    val id: String, val clientSyncId: String, val date: String, val startTime: String,
    val durationMinutes: Int, val type: String, val muscles: List<String> = emptyList(),
    val exercises: List<JournalExercise> = emptyList(), val energy: Int = 3, val effort: Int = 3,
    val whatIDid: String = "", val howIFelt: String = "", val whyGoal: String = "",
    val notes: String = "", val routineId: String? = null, val createdAt: String, val updatedAt: String
)

data class JournalSession(val id: String, val date: String, val title: String, val seconds: Int, val volume: Double)
data class JournalDay(val date: String, val entries: List<JournalEntry>, val sessions: List<JournalSession>) {
    val seconds get() = entries.sumOf { it.durationMinutes * 60L } + sessions.sumOf { it.seconds.toLong() }
    val volume get() = entries.sumOf { it.exercises.sumOf { e -> e.sets * e.reps * e.kg } } + sessions.sumOf { it.volume }
    val hasActivity get() = entries.isNotEmpty() || sessions.isNotEmpty()
}
data class JournalMonthStats(val days: Int, val minutes: Long, val averageMinutes: Long, val volume: Double)
data class StrengthPoint(val date: String, val estimatedMax: Double)
data class JournalWeek(val start: String, val minutes: Long, val volume: Double)

fun journalZone(): TimeZone = runCatching { TimeZone.currentSystemDefault() }.getOrElse { TimeZone.of("America/Lima") }
fun journalToday(): LocalDate = Clock.System.now().toLocalDateTime(journalZone()).date
fun estimatedOneRepMax(kg: Double, reps: Int): Double =
    if (!kg.isFinite() || kg <= 0 || reps <= 0) 0.0 else kg * (1.0 + reps / 30.0)

fun validateJournal(entry: JournalEntry, today: LocalDate = journalToday()): List<String> = buildList {
    val date = runCatching { LocalDate.parse(entry.date) }.getOrNull()
    if (date == null || date > today) add("Elige una fecha válida que no sea futura.")
    if (runCatching { LocalTime.parse(entry.startTime) }.isFailure) add("Usa una hora válida: HH:mm.")
    if (entry.durationMinutes !in 1..1440) add("La duración debe estar entre 1 minuto y 24 horas.")
    if (entry.type !in listOf("Fuerza", "Cardio", "Clase", "Otro")) add("Selecciona un tipo de entrenamiento.")
    if (entry.energy !in 1..5 || entry.effort !in 1..5) add("Energía y esfuerzo deben estar entre 1 y 5.")
    if (listOf(entry.whatIDid, entry.howIFelt, entry.whyGoal, entry.notes).any { it.length > 1000 }) add("Cada texto admite hasta 1000 caracteres.")
    if (entry.exercises.size > 30 || entry.exercises.any { it.name.isBlank() || it.name.length > 100 || it.sets !in 1..100 || it.reps !in 1..1000 || !it.kg.isFinite() || it.kg !in 0.0..1000.0 })
        add("Revisa los ejercicios: nombre, series y repeticiones positivos, y peso entre 0 y 1000 kg.")
}

fun journalStreak(dates: Set<LocalDate>, today: LocalDate): Int {
    var cursor = if (today in dates) today else today.minus(1, DateTimeUnit.DAY)
    var count = 0
    while (cursor in dates) { count++; cursor = cursor.minus(1, DateTimeUnit.DAY) }
    return count
}

fun journalStats(days: List<JournalDay>, month: LocalDate): JournalMonthStats {
    val included = days.filter { val d = LocalDate.parse(it.date); d.year == month.year && d.monthNumber == month.monthNumber && it.hasActivity }
    val minutes = included.sumOf { it.seconds } / 60
    return JournalMonthStats(included.size, minutes, if (included.isEmpty()) 0 else minutes / included.size, included.sumOf { it.volume })
}

fun strengthTimeline(entries: List<JournalEntry>, exercise: String): List<StrengthPoint> = entries
    .flatMap { e -> e.exercises.filter { it.name.equals(exercise, true) }.map { e.date to estimatedOneRepMax(it.kg, it.reps) } }
    .groupBy({ it.first }, { it.second }).map { StrengthPoint(it.key, it.value.max()) }.sortedBy { it.date }

fun journalWeeks(days: List<JournalDay>): List<JournalWeek> = days.filter { it.hasActivity }.groupBy {
    val d = LocalDate.parse(it.date); d.minus(d.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
}.map { (week, values) -> JournalWeek(week.toString(), values.sumOf { it.seconds } / 60, values.sumOf { it.volume }) }.sortedBy { it.start }.takeLast(8)

/** Contrato reservado para sincronización idempotente futura; no se instancia ni llama en fase local. */
interface JournalSyncTransport { suspend fun upload(accountId: String, entry: JournalEntry): Result<Unit> }
