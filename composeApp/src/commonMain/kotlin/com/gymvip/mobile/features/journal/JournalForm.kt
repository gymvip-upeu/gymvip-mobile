package com.gymvip.mobile.features.journal

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.features.training.WorkoutRoutineDto
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlin.random.Random

private data class ExerciseDraft(val name: String = "", val sets: String = "3", val reps: String = "10", val kg: String = "0")

@Composable
internal fun JournalForm(initial: JournalEntry?, defaultDate: String, routines: List<WorkoutRoutineDto>, saving: Boolean, onCancel: () -> Unit, onSave: (JournalEntry) -> Unit) {
    var date by rememberSaveable { mutableStateOf(initial?.date ?: defaultDate) }
    var time by rememberSaveable { mutableStateOf(initial?.startTime ?: "08:00") }
    var hours by rememberSaveable { mutableStateOf(((initial?.durationMinutes ?: 0) / 60).toString()) }
    var minutes by rememberSaveable { mutableStateOf(((initial?.durationMinutes ?: 0) % 60).toString()) }
    var type by rememberSaveable { mutableStateOf(initial?.type ?: "Fuerza") }
    var muscles by rememberSaveable { mutableStateOf(initial?.muscles ?: emptyList<String>()) }
    val exerciseSaver = listSaver<List<ExerciseDraft>, String>(
        save = { list -> list.flatMap { listOf(it.name, it.sets, it.reps, it.kg) } },
        restore = { saved -> saved.chunked(4).map { ExerciseDraft(it[0], it[1], it[2], it[3]) } }
    )
    var exercises: List<ExerciseDraft> by rememberSaveable(stateSaver = exerciseSaver) { mutableStateOf<List<ExerciseDraft>>(initial?.exercises?.map { ExerciseDraft(it.name, it.sets.toString(), it.reps.toString(), it.kg.toString()) } ?: emptyList()) }
    var energy by rememberSaveable { mutableStateOf(initial?.energy ?: 3) }
    var effort by rememberSaveable { mutableStateOf(initial?.effort ?: 3) }
    var what by rememberSaveable { mutableStateOf(initial?.whatIDid.orEmpty()) }
    var feeling by rememberSaveable { mutableStateOf(initial?.howIFelt.orEmpty()) }
    var goal by rememberSaveable { mutableStateOf(initial?.whyGoal.orEmpty()) }
    var notes by rememberSaveable { mutableStateOf(initial?.notes.orEmpty()) }
    var routineId by rememberSaveable { mutableStateOf(initial?.routineId) }
    var errors by remember { mutableStateOf<List<String>>(emptyList()) }
    var timerStart by rememberSaveable { mutableStateOf<Long?>(null) }
    var elapsed by rememberSaveable { mutableStateOf(0L) }
    LaunchedEffect(timerStart) { while (timerStart != null) { elapsed = ((Clock.System.now().toEpochMilliseconds() - timerStart!!) / 1000).coerceAtLeast(0); delay(500) } }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("¿Cómo te fue hoy?", style = MaterialTheme.typography.headlineSmall)
        Text("Registra tu experiencia a tu ritmo. Los campos de ejercicios y notas son opcionales.")
        OutlinedTextField(date, { date = it.take(10) }, label = { Text("Fecha (AAAA-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(time, { time = it.take(5) }, label = { Text("Hora de inicio (HH:mm)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(hours, { hours = it.filter(Char::isDigit).take(2) }, label = { Text("Horas") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(2) }, label = { Text("Minutos (0–59)") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        OutlinedButton(onClick = {
            if (timerStart == null) { timerStart = Clock.System.now().toEpochMilliseconds() - elapsed * 1000 }
            else { timerStart = null; val total = (elapsed / 60).coerceAtLeast(1); hours = (total / 60).toString(); minutes = (total % 60).toString() }
        }) { Text(if (timerStart == null) "Iniciar cronómetro" else "Detener · ${elapsed / 60}:${(elapsed % 60).toString().padStart(2, '0')}") }
        Text("Tipo de entrenamiento", style = MaterialTheme.typography.titleMedium)
        JournalChips(listOf("Fuerza", "Cardio", "Clase", "Otro"), listOf(type)) { type = it }
        Text("Músculos trabajados", style = MaterialTheme.typography.titleMedium)
        JournalChips(listOf("Pecho", "Espalda", "Piernas", "Hombros", "Brazos", "Core", "Cuerpo completo"), muscles) { muscles = if (it in muscles) muscles - it else muscles + it }
        JournalRating("Energía", energy) { energy = it }; JournalRating("Esfuerzo percibido", effort) { effort = it }
        Text("Ejercicios opcionales", style = MaterialTheme.typography.titleMedium)
        exercises.forEachIndexed { index, draft ->
            JournalPanel {
                fun update(value: ExerciseDraft) { exercises = exercises.mapIndexed { i, e -> if (i == index) value else e } }
                OutlinedTextField(draft.name, { update(draft.copy(name = it.take(100))) }, label = { Text("Nombre del ejercicio") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(draft.sets, { update(draft.copy(sets = it.take(3))) }, label = { Text("Series") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(draft.reps, { update(draft.copy(reps = it.take(4))) }, label = { Text("Reps") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(draft.kg, { update(draft.copy(kg = it.take(8))) }, label = { Text("Kg") }, modifier = Modifier.weight(1f))
                }
                TextButton(onClick = { exercises = exercises.filterIndexed { i, _ -> i != index } }) { Text("Quitar ejercicio") }
            }
        }
        OutlinedButton(onClick = { exercises = exercises + ExerciseDraft() }, enabled = exercises.size < 30) { Text("Agregar ejercicio") }
        JournalTextField("Qué hice", what) { what = it }; JournalTextField("Cómo me sentí", feeling) { feeling = it }
        JournalTextField("Por qué / objetivo de hoy", goal) { goal = it }; JournalTextField("Notas libres", notes) { notes = it }
        Text("Rutina vinculada (opcional)", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = routineId == null, onClick = { routineId = null }, label = { Text("Sin vínculo") })
            routines.forEach { routine -> FilterChip(selected = routineId == routine.id, onClick = { routineId = routine.id }, label = { Text(routine.title) }) }
        }
        if (routines.isEmpty()) Text("Las rutinas aparecerán cuando estén disponibles en Entreno.", style = MaterialTheme.typography.bodySmall)
        errors.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(enabled = !saving && timerStart == null, onClick = {
            val now = Clock.System.now().toString()
            val id = initial?.id ?: "journal-${Clock.System.now().toEpochMilliseconds()}-${Random.nextLong().toString(16)}"
            val entry = JournalEntry(id, initial?.clientSyncId ?: id, date, time, (hours.toIntOrNull() ?: 0) * 60 + (minutes.toIntOrNull() ?: 0), type, muscles,
                exercises.map { JournalExercise(it.name.trim(), it.sets.toIntOrNull() ?: 0, it.reps.toIntOrNull() ?: 0, it.kg.replace(',', '.').toDoubleOrNull() ?: Double.NaN) }, energy, effort, what, feeling, goal, notes, routineId, initial?.createdAt ?: now, now)
            errors = validateJournal(entry) + if (minutes.toIntOrNull() !in 0..59 || hours.toIntOrNull() == null) listOf("Revisa las horas y los minutos (0–59).") else emptyList()
            if (errors.isEmpty()) onSave(entry)
        }, modifier = Modifier.fillMaxWidth()) { Text(if (initial == null) "Guardar registro" else "Guardar cambios") }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun JournalTextField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value, { onChange(it.take(1000)) }, label = { Text(label) }, supportingText = { Text("${value.length}/1000") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 5)
}
@Composable
private fun JournalRating(label: String, value: Int, onChange: (Int) -> Unit) {
    Text("$label: $value/5", style = MaterialTheme.typography.titleMedium)
    JournalChips((1..5).map { it.toString() }, listOf(value.toString())) { onChange(it.toInt()) }
}
@Composable
private fun JournalChips(labels: List<String>, selected: List<String>, onClick: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { labels.forEach { label -> FilterChip(selected = label in selected, onClick = { onClick(label) }, label = { Text(label) }) } }
}
