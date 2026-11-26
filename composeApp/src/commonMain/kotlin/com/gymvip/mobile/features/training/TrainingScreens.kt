package com.gymvip.mobile.features.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.presentation.component.GuideHint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

private data class SetDraft(
    val reps: String,
    val weight: String,
    val done: Boolean
)

@Composable
fun TrainingHubScreen(
    accountId: String,
    repository: EnterpriseTrainingRepository,
    onOpenExercises: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenJournal: () -> Unit = {},
    onOpenClasses: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var routines by remember { mutableStateOf<List<WorkoutRoutineDto>>(emptyList()) }
    var activeRoutine by remember { mutableStateOf<WorkoutRoutineDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var elapsedSeconds by rememberSaveable { mutableStateOf(0) }
    var restSecondsRemaining by rememberSaveable { mutableStateOf(0) }
    var isPaused by rememberSaveable { mutableStateOf(false) }

    val setDrafts = remember { mutableStateMapOf<String, SetDraft>() }

    LaunchedEffect(accountId) {
        loading = true
        repository.fetchRoutines(accountId)
            .onSuccess { routines = it }
            .onFailure { statusMessage = it.message ?: "Sin conexión. Conéctate una vez para cachear tus rutinas." }
        loading = false
    }

    LaunchedEffect(activeRoutine != null, isPaused) {
        while (activeRoutine != null && !isPaused) {
            delay(1000)
            elapsedSeconds++
            if (restSecondsRemaining > 0) restSecondsRemaining--
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Centro de Entrenamiento", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        GuideHint(
            title = "Registro offline-first con temporizador automático",
            description = "Al marcar una serie completada inicia el descanso automático. Si pierdes señal en el gimnasio, tu sesión se guarda en SQLite y se sincroniza después."
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onOpenExercises, modifier = Modifier.weight(1f)) { Text("Ejercicios") }
            OutlinedButton(onClick = onOpenProgress, modifier = Modifier.weight(1f)) { Text("Progreso") }
            OutlinedButton(onClick = onOpenClasses, modifier = Modifier.weight(1f)) { Text("Clases") }
        }

        if (!statusMessage.isNullOrBlank()) {
            Text(statusMessage ?: "", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
        }

        OutlinedButton(onClick = onOpenJournal, modifier = Modifier.fillMaxWidth()) { Text("Mi Bitácora · registrar mi día") }

        val current = activeRoutine
        if (current != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SESIÓN EN CURSO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(current.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Tiempo: ${elapsedSeconds / 60}m ${elapsedSeconds % 60}s", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { isPaused = !isPaused }) {
                            Icon(
                                if (isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                                contentDescription = if (isPaused) "Reanudar sesión" else "Pausar sesión"
                            )
                        }
                    }

                    if (restSecondsRemaining > 0) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Timer, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Descanso: ${restSecondsRemaining}s", fontWeight = FontWeight.Bold)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(onClick = { restSecondsRemaining += 15 }) { Text("+15s") }
                                    OutlinedButton(onClick = { restSecondsRemaining = 0 }) { Text("Saltar") }
                                }
                            }
                        }
                    }

                    current.items.forEach { item ->
                        Text("${item.exerciseName} (${item.setsCount} x ${item.repsTarget})", fontWeight = FontWeight.SemiBold)
                        for (setIdx in 1..item.setsCount) {
                            val key = "${item.id}_$setIdx"
                            val draft = setDrafts[key] ?: SetDraft("10", "20", false)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("S$setIdx", modifier = Modifier.width(26.dp))
                                OutlinedTextField(
                                    value = draft.reps,
                                    onValueChange = { setDrafts[key] = draft.copy(reps = it.filter(Char::isDigit)) },
                                    label = { Text("Reps") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = draft.weight,
                                    onValueChange = { setDrafts[key] = draft.copy(weight = it.filter { ch -> ch.isDigit() || ch == '.' }) },
                                    label = { Text("Kg") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Checkbox(
                                    checked = draft.done,
                                    onCheckedChange = { checked ->
                                        setDrafts[key] = draft.copy(done = checked)
                                        if (checked) restSecondsRemaining = item.restSeconds
                                    }
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = {
                            activeRoutine = null
                            elapsedSeconds = 0
                            restSecondsRemaining = 0
                        }, modifier = Modifier.weight(1f)) { Text("Cancelar") }

                        Button(onClick = {
                            scope.launch {
                                val loggedSets = current.items.flatMap { item ->
                                    (1..item.setsCount).mapNotNull { setIdx ->
                                        val draft = setDrafts["${item.id}_$setIdx"]
                                        if (draft?.done == true) {
                                            WorkoutSetPayload(
                                                exerciseId = item.exerciseId,
                                                exerciseName = item.exerciseName,
                                                setNumber = setIdx,
                                                reps = draft.reps.toIntOrNull() ?: 10,
                                                weightKg = draft.weight.toDoubleOrNull() ?: 0.0
                                            )
                                        } else null
                                    }
                                }
                                val nowIso = Clock.System.now().toString().take(19)
                                val syncId = "wrk_${accountId}_${Clock.System.now().toEpochMilliseconds()}"
                                val (_, msg) = repository.saveAndSyncWorkoutSession(
                                    accountId,
                                    WorkoutSessionPayload(
                                        routineId = current.id,
                                        routineTitle = current.title,
                                        durationSeconds = elapsedSeconds,
                                        notes = "Registrado desde GYM VIP Mobile",
                                        clientSyncId = syncId,
                                        startedAt = nowIso,
                                        completedAt = nowIso,
                                        sets = loggedSets
                                    )
                                )
                                statusMessage = msg
                                activeRoutine = null
                                elapsedSeconds = 0
                                restSecondsRemaining = 0
                            }
                        }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Finalizar")
                        }
                    }
                }
            }
        } else {
            if (loading) {
                CircularProgressIndicator()
            } else if (routines.isEmpty()) {
                Text("Aún no tienes rutinas cargadas en esta cuenta.")
            } else {
                routines.forEach { routine ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(routine.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Text("${routine.goal} • ${routine.level} • ~${routine.durationMinutes} min", style = MaterialTheme.typography.bodySmall)
                            if (!routine.notes.isNullOrBlank()) {
                                Text(routine.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            routine.items.forEach { ex ->
                                Text("• ${ex.exerciseName}: ${ex.setsCount} x ${ex.repsTarget} (desc. ${ex.restSeconds}s)", style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(onClick = {
                                activeRoutine = routine
                                elapsedSeconds = 0
                                restSecondsRemaining = 0
                                isPaused = false
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text("Iniciar entrenamiento")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseCatalogScreen(
    accountId: String,
    repository: EnterpriseTrainingRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var filter by remember { mutableStateOf("Todos") }
    var exercises by remember { mutableStateOf<List<ExerciseDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(filter) {
        loading = true
        repository.fetchExercises(accountId, if (filter == "Todos") null else filter)
            .onSuccess { exercises = it }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catálogo de Ejercicios") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Todos", "Piernas", "Pecho", "Espalda", "Core").forEach { group ->
                    FilterChip(selected = filter == group, onClick = { filter = group }, label = { Text(group) })
                }
            }
            if (loading) CircularProgressIndicator()
            exercises.forEach { ex ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(ex.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text("${ex.muscleGroup} • ${ex.equipment} • ${ex.difficulty}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text("Técnica: ${ex.technique}", style = MaterialTheme.typography.bodySmall)
                        if (!ex.warnings.isNullOrBlank()) {
                            Text("Advertencia: ${ex.warnings}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingProgressScreen(
    accountId: String,
    repository: EnterpriseTrainingRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var summary by remember { mutableStateOf(WorkoutProgressDto()) }
    var pendingSync by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }

    suspend fun reload() {
        loading = true
        summary = repository.fetchProgress(accountId)
        pendingSync = repository.pendingWorkoutCount(accountId)
        loading = false
    }

    LaunchedEffect(accountId) { reload() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Progreso y Récords") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (loading) {
                CircularProgressIndicator()
            } else {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Resumen real de entrenamientos", fontWeight = FontWeight.Bold)
                        Text("Sesiones completadas: ${summary.totalSessions}")
                        Text("Volumen acumulado: ${summary.totalVolumeKg} kg")
                        if (pendingSync > 0) {
                            Text("Pendientes de sincronizar: $pendingSync", color = MaterialTheme.colorScheme.primary)
                            Button(onClick = { scope.launch { reload() } }) { Text("Sincronizar ahora") }
                        }
                    }
                }

                if (summary.personalRecords.isNotEmpty()) {
                    Text("Récords personales registrados", fontWeight = FontWeight.Bold)
                    summary.personalRecords.forEach { pr ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(pr.exerciseName, fontWeight = FontWeight.SemiBold)
                                Text("${pr.maxWeightKg} kg • ${pr.maxReps} reps")
                            }
                        }
                    }
                }

                if (summary.recentSessions.isEmpty()) {
                    Text("Aún no registraste sesiones. Completa una rutina desde Entreno para ver tu evolución real aquí.")
                } else {
                    Text("Historial reciente", fontWeight = FontWeight.Bold)
                    summary.recentSessions.forEach { s ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(s.routineTitle, fontWeight = FontWeight.SemiBold)
                                Text("${s.durationSeconds / 60} min • Volumen ${s.totalVolumeKg} kg • ${s.completedAt.take(10)}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
