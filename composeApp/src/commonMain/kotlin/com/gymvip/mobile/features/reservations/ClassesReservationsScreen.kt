package com.gymvip.mobile.features.reservations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.features.training.EnterpriseTrainingRepository
import com.gymvip.mobile.features.training.GymClassDto
import com.gymvip.mobile.presentation.component.GuideHint
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassesReservationsScreen(
    repository: EnterpriseTrainingRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var classes by remember { mutableStateOf<List<GymClassDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var feedback by remember { mutableStateOf<String?>(null) }

    suspend fun loadClasses() {
        loading = true
        repository.listClasses()
            .onSuccess { classes = it }
            .onFailure { feedback = it.message ?: "No se pudieron cargar las clases disponibles." }
        loading = false
    }

    LaunchedEffect(Unit) { loadClasses() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clases y Reservas") },
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
            GuideHint(
                title = "Validación de cupos en servidor",
                description = "Toda reserva verifica tu membresía vigente y disponibilidad de cupos en tiempo real para evitar sobreocupación o reservas duplicadas."
            )

            if (!feedback.isNullOrBlank()) {
                Text(feedback ?: "", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }

            if (loading) {
                CircularProgressIndicator()
            } else if (classes.isEmpty()) {
                Text("No hay clases programadas en este momento.")
            } else {
                classes.forEach { item ->
                    val available = (item.capacity - item.reservedCount).coerceAtLeast(0)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${item.category} • ${item.trainerName}", style = MaterialTheme.typography.bodySmall)
                            if (!item.branchName.isNullOrBlank()) {
                                Text(item.branchName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("Horario: ${item.startsAt.replace("T", " ").take(16)} (${item.durationMinutes} min)", style = MaterialTheme.typography.bodySmall)
                            Text("Cupos disponibles: $available / ${item.capacity}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(6.dp))
                            if (item.reservedByMe) {
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        repository.cancelClass(item.id)
                                            .onSuccess { feedback = "Reserva cancelada."; loadClasses() }
                                            .onFailure { feedback = it.message }
                                    }
                                }, modifier = Modifier.fillMaxWidth()) { Text("Cancelar reserva") }
                            } else {
                                Button(onClick = {
                                    scope.launch {
                                        repository.reserveClass(item.id)
                                            .onSuccess { res ->
                                                val st = res["status"]?.toString()?.replace("\"", "") ?: "RESERVADA"
                                                feedback = if (st == "LISTA_ESPERA") "Clase llena: añadido a lista de espera." else "Cupo reservado con éxito."
                                                loadClasses()
                                            }
                                            .onFailure { feedback = it.message ?: "No se pudo completar la reserva." }
                                    }
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Text(if (available > 0) "Reservar cupo" else "Unirse a lista de espera")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
