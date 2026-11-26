package com.gymvip.mobile.features.gyms

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
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.features.training.EnterpriseTrainingRepository
import com.gymvip.mobile.features.training.PublishedBranchDto
import com.gymvip.mobile.features.training.PublishedGymDto
import com.gymvip.mobile.presentation.component.GuideHint
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymsExplorerScreen(
    accountId: String,
    preferredBranchName: String,
    repository: EnterpriseTrainingRepository,
    onBranchSelected: (PublishedBranchDto) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var districtFilter by remember { mutableStateOf("Todos") }
    var gyms by remember { mutableStateOf<List<PublishedGymDto>>(emptyList()) }
    var favorites by remember { mutableStateOf(repository.getFavoriteGymIds(accountId)) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var noticeMsg by remember { mutableStateOf<String?>(null) }

    suspend fun loadGyms() {
        loading = true
        errorMsg = null
        val dist = if (districtFilter == "Todos") null else districtFilter
        repository.fetchGyms(query = query.ifBlank { null }, district = dist, scopeId = accountId)
            .onSuccess { gyms = it }
            .onFailure { errorMsg = it.message ?: "No se pudieron cargar los gimnasios." }
        loading = false
    }

    LaunchedEffect(Unit) { loadGyms() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Explorar Gimnasios y Sedes") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GuideHint(
                title = "Gimnasio favorito vs membresía",
                description = "Marcar una sede preferida personaliza tu Inicio y horarios, pero tu permiso de ingreso depende exclusivamente de tu membresía vigente."
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Buscar gimnasio, sede o distrito") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Todos", "Miraflores", "San Borja").forEach { dist ->
                    FilterChip(
                        selected = districtFilter == dist,
                        onClick = {
                            districtFilter = dist
                            scope.launch { loadGyms() }
                        },
                        label = { Text(dist) }
                    )
                }
                Button(onClick = { scope.launch { loadGyms() } }) { Text("Buscar") }
            }

            if (!noticeMsg.isNullOrBlank()) {
                Text(noticeMsg ?: "", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            if (!errorMsg.isNullOrBlank()) {
                Text(errorMsg ?: "", color = MaterialTheme.colorScheme.error)
            }

            if (loading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            } else if (gyms.isEmpty()) {
                Text("No se encontraron gimnasios publicados con esos filtros.")
            } else {
                gyms.forEach { gym ->
                    val isFav = gym.id in favorites
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(gym.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    if (!gym.description.isNullOrBlank()) {
                                        Text(gym.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (gym.minPricePen != null && gym.maxPricePen != null) {
                                        Text(
                                            "Planes desde S/ ${gym.minPricePen} hasta S/ ${gym.maxPricePen}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                IconButton(onClick = {
                                    scope.launch { favorites = repository.toggleFavoriteGym(accountId, gym.id) }
                                }) {
                                    Icon(
                                        imageVector = if (isFav) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Marcar gimnasio favorito",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            gym.branches.forEach { branch ->
                                val isPreferred = branch.name == preferredBranchName
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(branch.name, fontWeight = FontWeight.SemiBold)
                                        }
                                        Text("${branch.district}, ${branch.city} • ${branch.address}", style = MaterialTheme.typography.bodySmall)
                                        if (!branch.scheduleSummary.isNullOrBlank()) {
                                            Text("Horarios: ${branch.scheduleSummary}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        if (!branch.servicesSummary.isNullOrBlank()) {
                                            Text("Servicios: ${branch.servicesSummary}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        if (isPreferred) {
                                            OutlinedButton(onClick = {}, enabled = false) {
                                                Text("Sede preferida actual")
                                            }
                                        } else {
                                            Button(onClick = {
                                                scope.launch {
                                                    repository.selectPreferredBranch(branch)
                                                    onBranchSelected(branch)
                                                    noticeMsg = "Sede preferida actualizada: ${branch.name}"
                                                }
                                            }) {
                                                Text("Elegir como sede preferida")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
