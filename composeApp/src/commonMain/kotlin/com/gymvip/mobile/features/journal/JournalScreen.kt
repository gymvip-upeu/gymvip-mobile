package com.gymvip.mobile.features.journal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.features.training.WorkoutRoutineDto
import com.gymvip.mobile.presentation.component.VipTopAppBar
import com.gymvip.mobile.presentation.navigation.AppBackHandler
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlin.random.Random

@Composable
fun JournalScreen(accountId: String, memberName: String, repository: JournalRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var days by remember(accountId) { mutableStateOf<List<JournalDay>>(emptyList()) }
    var routines by remember(accountId) { mutableStateOf<List<WorkoutRoutineDto>>(emptyList()) }
    var error by remember(accountId) { mutableStateOf<String?>(null) }
    var loading by remember(accountId) { mutableStateOf(true) }
    var revision by remember { mutableStateOf(0) }
    var selectedDate by remember(accountId) { mutableStateOf<String?>(null) }
    var editing by remember(accountId) { mutableStateOf<JournalEntry?>(null) }
    var form by remember(accountId) { mutableStateOf(false) }
    var sharing by remember(accountId) { mutableStateOf(false) }
    var savingEntry by remember(accountId) { mutableStateOf(false) }
    var deleteId by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf(0) }
    var month by remember { mutableStateOf(LocalDate(journalToday().year, journalToday().monthNumber, 1)) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(accountId, revision) {
        loading = true
        try { days = repository.days(accountId); routines = repository.routines(accountId); error = null }
        catch (e: Exception) { error = "No se pudo leer tu bitácora. Tus datos se conservan. Intenta de nuevo." }
        finally { loading = false }
    }
    val day = days.firstOrNull { it.date == selectedDate } ?: JournalDay(selectedDate ?: journalToday().toString(), emptyList(), emptyList())
    fun back() { when { sharing -> sharing = false; form -> form = false; selectedDate != null -> selectedDate = null; else -> onBack() } }
    AppBackHandler(enabled = true) { back() }
    Scaffold(topBar = { VipTopAppBar(title = if (sharing) "Compartir mi día" else "Mi Bitácora") }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            TextButton(onClick = { back() }) { Text("‹ Volver") }
            when {
                loading -> CircularProgressIndicator()
                error != null -> { Text(error!!, color = MaterialTheme.colorScheme.error); Button(onClick = { revision++ }) { Text("Reintentar") } }
                sharing -> JournalShareScreen(day, days, memberName) { message -> scope.launch { snackbar.showSnackbar(message) } }
                form -> JournalForm(editing, day.date, routines, savingEntry, onCancel = { form = false }) { entry ->
                    if (savingEntry) return@JournalForm
                    savingEntry = true
                    scope.launch {
                        try { repository.save(accountId, entry); form = false; selectedDate = entry.date; revision++; snackbar.showSnackbar("Registro guardado en este dispositivo") }
                        catch (e: Exception) { snackbar.showSnackbar("No se pudo guardar. Revisa el registro e inténtalo de nuevo.") }
                        finally { savingEntry = false }
                    }
                }
                selectedDate != null -> Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(day.date, style = MaterialTheme.typography.headlineSmall)
                    Text("${day.seconds / 60} min · ${day.volume.toLong()} kg de volumen")
                    if (!day.hasActivity) Text("Este día aún no tiene registros. Puedes agregar cómo te fue.")
                    day.sessions.forEach { session -> JournalPanel { Text("Sesión de rutina", color = MaterialTheme.colorScheme.primary); Text(session.title, style = MaterialTheme.typography.titleMedium); Text("${session.seconds / 60} min · ${session.volume.toLong()} kg · Solo lectura") } }
                    day.entries.forEach { entry -> JournalPanel {
                        Text("${entry.type} · ${entry.startTime} · ${entry.durationMinutes} min", style = MaterialTheme.typography.titleMedium)
                        Text(entry.muscles.joinToString(" · "))
                        Text("Energía ${entry.energy}/5 · Esfuerzo ${entry.effort}/5")
                        listOf("Qué hice" to entry.whatIDid, "Cómo me sentí" to entry.howIFelt, "Objetivo" to entry.whyGoal, "Notas" to entry.notes).filter { it.second.isNotBlank() }.forEach { Text("${it.first}: ${it.second}") }
                        entry.exercises.forEach { Text("${it.name}: ${it.sets} × ${it.reps} · ${it.kg} kg") }
                        Row { TextButton(onClick = { editing = entry; form = true }) { Text("Editar") }; TextButton(onClick = { deleteId = entry.id }) { Text("Eliminar") } }
                    } }
                    Button(onClick = { editing = null; form = true }, enabled = LocalDate.parse(day.date) <= journalToday(), modifier = Modifier.fillMaxWidth()) { Text("Agregar registro") }
                    OutlinedButton(onClick = { sharing = true }, enabled = day.hasActivity, modifier = Modifier.fillMaxWidth()) { Text("Compartir mi día") }
                    Spacer(Modifier.height(24.dp))
                }
                else -> {
                    TabRow(selectedTabIndex = tab) { listOf("Calendario", "Evolución").forEachIndexed { index, label -> Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) }) } }
                    if (tab == 0) Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Spacer(Modifier.height(8.dp))
                        Text("Tu constancia, día a día", style = MaterialTheme.typography.headlineSmall)
                        Text("Registra lo que hiciste y cómo te sentiste. Disponible sin conexión.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val stats = journalStats(days, month)
                        val streak = journalStreak(days.map { LocalDate.parse(it.date) }.toSet(), journalToday())
                        JournalPanel {
                            Text("Racha actual: $streak ${if (streak == 1) "día" else "días"}", style = MaterialTheme.typography.titleMedium)
                            Text("${stats.days} ${if (stats.days == 1) "día entrenado" else "días entrenados"} · ${stats.minutes / 60} h ${stats.minutes % 60} min")
                            Text("Promedio: ${stats.averageMinutes} min por día entrenado")
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { month = month.minus(1, DateTimeUnit.MONTH) }) { Text("‹ Anterior") }
                            Text("${monthNames[month.monthNumber - 1]} ${month.year}", style = MaterialTheme.typography.titleSmall)
                            TextButton(onClick = { month = month.plus(1, DateTimeUnit.MONTH) }) { Text("Siguiente ›") }
                        }
                        val offset = month.dayOfWeek.isoDayNumber - 1
                        val count = month.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).dayOfMonth
                        Row { listOf("L", "M", "X", "J", "V", "S", "D").forEach { Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(it) } } }
                        for (row in 0 until (offset + count + 6) / 7) Row(Modifier.fillMaxWidth()) {
                            repeat(7) { col ->
                                val n = row * 7 + col - offset + 1
                                val date = if (n in 1..count) LocalDate(month.year, month.monthNumber, n) else null
                                val active = days.any { it.date == date?.toString() && it.hasActivity }
                                val bg = if (date == journalToday()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                Box(Modifier.weight(1f).heightIn(min = 48.dp).background(bg).then(if (date != null) Modifier.clickable { selectedDate = date.toString() } else Modifier), contentAlignment = Alignment.Center) {
                                    if (date != null) Text("$n${if (active) " •" else ""}", color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                        Button(onClick = { selectedDate = journalToday().toString(); editing = null; form = true }, modifier = Modifier.fillMaxWidth()) { Text("¿Cómo te fue hoy?") }
                        if (days.isEmpty()) Text("Tu historia empieza con el primer registro. Entrena a tu ritmo.")
                        Spacer(Modifier.height(24.dp))
                    } else JournalEvolution(days)
                }
            }
        }
    }
    if (deleteId != null) AlertDialog(onDismissRequest = { deleteId = null }, title = { Text("¿Eliminar este registro?") }, text = { Text("Esta acción elimina solo el registro manual de tu bitácora.") }, confirmButton = { TextButton(onClick = {
        val id = deleteId!!; deleteId = null
        scope.launch { try { repository.delete(accountId, id); revision++; snackbar.showSnackbar("Registro eliminado") } catch (e: Exception) { snackbar.showSnackbar("No se pudo eliminar el registro") } }
    }) { Text("Eliminar") } }, dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Cancelar") } })
}

internal val monthNames = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")

@Composable
internal fun JournalPanel(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun JournalEvolution(days: List<JournalDay>) {
    val entries = days.flatMap { it.entries }
    val exercises = entries.flatMap { it.exercises }.map { it.name }.distinct().sorted()
    var exercise by remember(exercises) { mutableStateOf(exercises.firstOrNull().orEmpty()) }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Evolución personal", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
        Text("Observa tus propios registros a lo largo del tiempo.")
        if (days.isEmpty()) Text("Cuando registres tus entrenamientos verás aquí tu evolución.")
        else {
            val current = journalStats(days, journalToday()); val previous = journalStats(days, journalToday().minus(1, DateTimeUnit.MONTH))
            JournalPanel { Text("Este mes frente al anterior", style = MaterialTheme.typography.titleMedium); Text("Días: ${current.days} / ${previous.days} · Minutos: ${current.minutes} / ${previous.minutes}"); Text("Volumen: ${current.volume.toLong()} / ${previous.volume.toLong()} kg") }
            Text("Fuerza por ejercicio · 1RM estimado", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { exercises.forEach { name -> FilterChip(selected = exercise == name, onClick = { exercise = name }, label = { Text(name) }) } }
            val points = strengthTimeline(entries, exercise)
            if (points.isEmpty()) Text("Agrega ejercicios con repeticiones y peso para ver una estimación de fuerza.")
            else { JournalChart(points.map { it.estimatedMax.toFloat() }, false); points.takeLast(8).forEach { Text("${it.date}: ${it.estimatedMax.toInt()} kg estimados") }; Text("Estimación Epley basada en tus registros; no es una indicación de peso a levantar.", style = MaterialTheme.typography.bodySmall) }
            val weeks = journalWeeks(days)
            Text("Tiempo por semana (min)", style = MaterialTheme.typography.titleMedium)
            JournalChart(weeks.map { it.minutes.toFloat() }, true)
            Text("Volumen por semana (kg)", style = MaterialTheme.typography.titleMedium)
            JournalChart(weeks.map { it.volume.toFloat() }, true)
            weeks.forEach { Text("Semana ${it.start}: ${it.minutes} min · ${it.volume.toLong()} kg") }
            Text("Las sesiones de rutina aportan tiempo y volumen; la estimación por ejercicio usa los registros manuales disponibles.", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun JournalChart(values: List<Float>, bars: Boolean) {
    val color = MaterialTheme.colorScheme.primary
    val axis = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.fillMaxWidth().height(140.dp)) {
        drawLine(axis, Offset(0f, size.height - 4), Offset(size.width, size.height - 4), 2f)
        val max = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val step = size.width / values.size.coerceAtLeast(1)
        values.forEachIndexed { i, v ->
            val point = Offset(step * (i + .5f), size.height - 8 - v / max * (size.height - 20))
            if (bars) drawLine(color, Offset(point.x, size.height - 8), point, step * .5f)
            else { drawCircle(color, 5.dp.toPx(), point); if (i > 0) drawLine(color, Offset(step * (i - .5f), size.height - 8 - values[i - 1] / max * (size.height - 20)), point, 3.dp.toPx()) }
        }
    }
}
