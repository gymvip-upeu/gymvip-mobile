package com.gymvip.mobile.presentation.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.features.saas.SaasExperienceApi
import com.gymvip.mobile.features.training.EnterpriseTrainingRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Composable
internal fun MemberOverview(
    accountId: String,
    training: EnterpriseTrainingRepository,
    wellness: SaasExperienceApi,
    onTraining: () -> Unit,
    onClasses: () -> Unit,
    onProgress: () -> Unit
) {
    var routine by remember(accountId) { mutableStateOf<String?>(null) }
    var upcoming by remember(accountId) { mutableStateOf<String?>(null) }
    var progress by remember(accountId) { mutableStateOf<String?>(null) }
    var bmi by remember(accountId) { mutableStateOf<String?>(null) }

    LaunchedEffect(accountId) {
        coroutineScope {
            val routines = async { training.fetchRoutines(accountId) }
            val classes = async { training.listClasses() }
            val summary = async { training.fetchProgress(accountId) }
            val physical = async { wellness.getPhysicalProfile() }
            routine = routines.await().fold(
                onSuccess = { it.firstOrNull()?.title ?: "Aún no tienes rutinas disponibles." },
                onFailure = { "No se pudieron consultar tus rutinas." }
            )
            upcoming = classes.await().fold(
                onSuccess = { list ->
                    list.filter { it.myReservationStatus == "RESERVADA" || (it.myReservationStatus == null && it.reservedByMe) }
                        .minByOrNull { it.startsAt }?.let {
                            it.title + " · " + it.startsAt.replace('T', ' ').take(16) + " (Lima)"
                        } ?: "Aún no tienes clases reservadas."
                },
                onFailure = { "No se pudieron consultar tus reservas." }
            )
            progress = summary.await().let { it.totalSessions.toString() + " sesiones registradas" }
            bmi = physical.await().getOrNull()?.takeIf { it.configured }?.bmiValue?.toString()
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Tu actividad", style = MaterialTheme.typography.titleLarge)
            OverviewRow("Rutina disponible", routine, "Entrenar", onTraining)
            OverviewRow("Próxima clase", upcoming, "Ver clases", onClasses)
            OverviewRow("Progreso", progress, "Ver progreso", onProgress)
            bmi?.let { Text("IMC informativo · " + it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun OverviewRow(title: String, value: String?, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(value ?: "Consultando…", style = MaterialTheme.typography.bodyMedium)
        }
        TextButton(onClick = onClick) { Text(action) }
    }
}
