package com.gymvip.mobile.features.saas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellnessPlannerScreen(
    saasApi: SaasExperienceApi,
    onOpenExercises: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var heightText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("FUERZA_GENERAL") }
    var level by remember { mutableStateOf("PRINCIPIANTE") }
    var weeklyDays by remember { mutableStateOf("3") }
    var planTitle by remember { mutableStateOf("Mi Semana Balanceada") }
    var mondayFocus by remember { mutableStateOf("Tren Inferior y Core") }
    var wednesdayFocus by remember { mutableStateOf("Empuje Superior") }
    var fridayFocus by remember { mutableStateOf("Tracción y Cardio Moderado") }
    var savedPlans by remember { mutableStateOf<List<WeeklyTrainingPlanDto>>(emptyList()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var releaseInfo by remember { mutableStateOf<AppReleaseManifestDto?>(null) }

    val bmiPreview = remember(heightText, weightText) {
        val h = heightText.toDoubleOrNull()
        val w = weightText.toDoubleOrNull()
        runCatching { EducationalBmiCalculator.evaluate(h, w) }.getOrNull()
    }

    LaunchedEffect(Unit) {
        saasApi.getPhysicalProfile().onSuccess { p ->
            if (p.configured) {
                heightText = p.heightCm?.toString().orEmpty()
                weightText = p.weightKg?.toString().orEmpty()
                goal = p.primaryGoal ?: goal
                level = p.experienceLevel ?: level
                weeklyDays = (p.weeklyAvailabilityDays ?: 3).toString()
            }
        }
        saasApi.listWeeklyPlans().onSuccess { savedPlans = it }
        saasApi.getLatestAppRelease(currentVersionCode = 200).onSuccess { releaseInfo = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil Físico & Plan Semanal") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            releaseInfo?.takeIf { it.updateAvailable }?.let { rel ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Nueva versión disponible: v${rel.versionName}", fontWeight = FontWeight.Bold)
                        Text(rel.releaseNotes, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            statusMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }
            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Perfil Físico Voluntario & IMC Referencial", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        EducationalBmiCalculator.DISCLAIMER,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = heightText,
                            onValueChange = { heightText = it },
                            label = { Text("Estatura (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it },
                            label = { Text("Peso (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    bmiPreview?.let { eval ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Text("IMC calculado: ${eval.bmi} — ${eval.categoryLabel}", fontWeight = FontWeight.Bold)
                                Text(eval.disclaimer, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("FUERZA_GENERAL", "RESISTENCIA", "SALUD_ACTIVA").forEach { option ->
                            FilterChip(selected = goal == option, onClick = { goal = option }, label = { Text(option.replace('_', ' ')) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            scope.launch {
                                errorMessage = null
                                statusMessage = null
                                saasApi.savePhysicalProfile(
                                    SavePhysicalProfileRequest(
                                        heightCm = heightText.toDoubleOrNull(),
                                        weightKg = weightText.toDoubleOrNull(),
                                        primaryGoal = goal,
                                        experienceLevel = level,
                                        weeklyAvailabilityDays = weeklyDays.toIntOrNull() ?: 3,
                                        consentSaved = true
                                    )
                                ).onSuccess {
                                    statusMessage = "Perfil físico opcional guardado correctamente."
                                }.onFailure {
                                    errorMessage = it.message ?: "No se pudo guardar el perfil físico."
                                }
                            }
                        }, modifier = Modifier.weight(1f)) {
                            Text("Guardar perfil")
                        }
                        OutlinedButton(onClick = {
                            scope.launch {
                                saasApi.deletePhysicalProfile().onSuccess {
                                    heightText = ""
                                    weightText = ""
                                    statusMessage = "Datos físicos eliminados por privacidad."
                                }
                            }
                        }) {
                            Text("Eliminar")
                        }
                    }
                }
            }

            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Planificador Semanal Adaptativo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = planTitle, onValueChange = { planTitle = it }, label = { Text("Nombre del plan semanal") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = mondayFocus, onValueChange = { mondayFocus = it }, label = { Text("Lunes") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = wednesdayFocus, onValueChange = { wednesdayFocus = it }, label = { Text("Miércoles") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = fridayFocus, onValueChange = { fridayFocus = it }, label = { Text("Viernes") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            scope.launch {
                                val daysPayload = """[{"day":"LUN","focus":"$mondayFocus"},{"day":"MIE","focus":"$wednesdayFocus"},{"day":"VIE","focus":"$fridayFocus"}]"""
                                saasApi.saveWeeklyPlan(
                                    SaveWeeklyPlanRequest(
                                        title = planTitle,
                                        goal = goal,
                                        level = level,
                                        daysJson = daysPayload
                                    )
                                ).onSuccess { saved ->
                                    savedPlans = listOf(saved) + savedPlans.filterNot { it.id == saved.id }
                                    statusMessage = "Plan semanal guardado."
                                }.onFailure { errorMessage = it.message }
                            }
                        }, modifier = Modifier.weight(1f)) {
                            Text("Guardar semana")
                        }
                        OutlinedButton(onClick = onOpenExercises) {
                            Text("Ver ejercicios")
                        }
                    }
                    savedPlans.forEach { plan ->
                        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Text(plan.title, fontWeight = FontWeight.SemiBold)
                                Text("${plan.goal} · ${plan.level}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
