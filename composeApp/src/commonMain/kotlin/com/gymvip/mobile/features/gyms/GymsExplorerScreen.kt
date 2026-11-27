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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.features.saas.CreateGymReviewRequest
import com.gymvip.mobile.features.saas.GroupPromotionDto
import com.gymvip.mobile.features.saas.GymReviewDto
import com.gymvip.mobile.features.saas.PublicGymShowcaseDto
import com.gymvip.mobile.features.saas.SaasExperienceApi
import com.gymvip.mobile.features.training.EnterpriseTrainingRepository
import com.gymvip.mobile.features.training.PublishedBranchDto
import com.gymvip.mobile.presentation.component.GuideHint
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymsExplorerScreen(
    accountId: String,
    preferredBranchName: String,
    repository: EnterpriseTrainingRepository,
    saasApi: SaasExperienceApi,
    canReview: Boolean = false,
    onBranchSelected: (PublishedBranchDto) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val links = LocalUriHandler.current
    var query by remember { mutableStateOf("") }
    var districtFilter by remember { mutableStateOf("") }
    var cityFilter by remember { mutableStateOf("") }
    var minPrice by remember { mutableStateOf("") }
    var maxPrice by remember { mutableStateOf("") }
    var amenity by remember { mutableStateOf("") }
    var filtersOpen by remember { mutableStateOf(false) }
    var gyms by remember { mutableStateOf<List<PublicGymShowcaseDto>>(emptyList()) }
    var compareIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var activeReviewsGymId by remember { mutableStateOf<String?>(null) }
    var reviews by remember { mutableStateOf<List<GymReviewDto>>(emptyList()) }
    var promos by remember { mutableStateOf<List<GroupPromotionDto>>(emptyList()) }
    var reviewComment by remember { mutableStateOf("") }
    var reviewRating by remember { mutableStateOf(5) }
    var favorites by remember { mutableStateOf(repository.getFavoriteGymIds(accountId)) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var noticeMsg by remember { mutableStateOf<String?>(null) }

    suspend fun loadGyms() {
        loading = true
        errorMsg = null
        val minimum = minPrice.trim().replace(',', '.')
        val maximum = maxPrice.trim().replace(',', '.')
        val pricePattern = Regex("[0-9]{1,6}(\\.[0-9]{1,2})?")
        if (listOf(minimum, maximum).any { it.isNotEmpty() && !pricePattern.matches(it) }
            || (minimum.toDoubleOrNull() ?: 0.0) > (maximum.toDoubleOrNull() ?: Double.MAX_VALUE)) {
            errorMsg = "Ingresa un rango de precios válido en soles."
            loading = false
            return
        }
        saasApi.listPublicGymsShowcase(query.trim(), districtFilter.trim(),
            cityFilter.trim(), minimum, maximum, amenity.trim())
            .onSuccess { gyms = it }
            .onFailure {
                gyms = emptyList()
                errorMsg = "No se pudo cargar el directorio. Intenta nuevamente."
            }
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
            OutlinedButton(onClick = {
                runCatching { links.openUri("https://gymvip.vercel.app/unete") }
                    .onFailure { noticeMsg = "No se pudo abrir el formulario de afiliación." }
            }) { Text("Afiliar mi empresa") }

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

            OutlinedButton(onClick = { filtersOpen = !filtersOpen }) {
                Text(if (filtersOpen) "Ocultar filtros" else "Filtros de búsqueda")
            }
            if (filtersOpen) {
                OutlinedTextField(cityFilter, { cityFilter = it }, label = { Text("Ciudad") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(districtFilter, { districtFilter = it }, label = { Text("Distrito") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(minPrice, { minPrice = it }, label = { Text("Desde (S/)") },
                        modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(maxPrice, { maxPrice = it }, label = { Text("Hasta (S/)") },
                        modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(amenity, { amenity = it }, label = { Text("Servicio o instalación") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            Button(onClick = { scope.launch { loadGyms() } }, enabled = !loading) { Text("Buscar") }

            if (!noticeMsg.isNullOrBlank()) {
                Text(noticeMsg ?: "", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            if (!errorMsg.isNullOrBlank()) {
                Text(errorMsg ?: "", color = MaterialTheme.colorScheme.error)
            }

            if (compareIds.size == 2) {
                val left = gyms.firstOrNull { it.id == compareIds[0] }
                val right = gyms.firstOrNull { it.id == compareIds[1] }
                if (left != null && right != null) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Comparador Directo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(left.name, fontWeight = FontWeight.Bold)
                                    Text("Desde: S/ ${left.minPricePen ?: "—"}")
                                    Text("Sedes: ${left.branches.size}")
                                    Text("Rating: ★ ${left.avgRating} (${left.reviewsCount})")
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(right.name, fontWeight = FontWeight.Bold)
                                    Text("Desde: S/ ${right.minPricePen ?: "—"}")
                                    Text("Sedes: ${right.branches.size}")
                                    Text("Rating: ★ ${right.avgRating} (${right.reviewsCount})")
                                }
                            }
                            OutlinedButton(onClick = { compareIds = emptyList() }) { Text("Limpiar comparador") }
                        }
                    }
                }
            }

            if (loading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            } else if (errorMsg != null && gyms.isEmpty()) {
                OutlinedButton(onClick = { scope.launch { loadGyms() } }) { Text("Reintentar") }
            } else if (gyms.isEmpty()) {
                Text("No se encontraron gimnasios publicados con esos filtros.")
            } else {
                gyms.forEach { gym ->
                    val isFav = gym.id in favorites
                    val isComparing = gym.id in compareIds
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(gym.headline ?: gym.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    val desc = gym.tagline ?: gym.description
                                    if (!desc.isNullOrBlank()) {
                                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("★ ${gym.avgRating} (${gym.reviewsCount} reseñas verificadas) · ${gym.activeGroupPromosCount} promos grupales",
                                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    compareIds = if (isComparing) compareIds - gym.id else (compareIds + gym.id).takeLast(2)
                                }) {
                                    Text(if (isComparing) "En comparador" else "Comparar")
                                }
                                OutlinedButton(onClick = {
                                        if (activeReviewsGymId == gym.id) {
                                            activeReviewsGymId = null
                                        } else {
                                            activeReviewsGymId = gym.id
                                            reviews = emptyList()
                                            promos = emptyList()
                                            scope.launch {
                                                saasApi.listGymReviews(gym.id).onSuccess { if (activeReviewsGymId == gym.id) reviews = it }
                                                saasApi.listGroupPromotions(gym.id).onSuccess { if (activeReviewsGymId == gym.id) promos = it }
                                            }
                                        }
                                    }) {
                                        Text(if (activeReviewsGymId == gym.id) "Ocultar detalles" else "Reseñas y Promos")
                                }
                            }
                            if (activeReviewsGymId == gym.id) {
                                promos.forEach { promo ->
                                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                        Column(Modifier.fillMaxWidth().padding(10.dp)) {
                                            Text("Promo Grupal: ${promo.title} (-${promo.discountPercent}%)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                            Text("${promo.minParticipants}-${promo.maxParticipants} personas · Vigente hasta ${promo.validUntil}", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                                if (reviews.isEmpty()) {
                                    Text("Aún no existen reseñas verificadas publicadas.", style = MaterialTheme.typography.bodySmall)
                                } else {
                                    reviews.forEach { r ->
                                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                            Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Text("★ ${r.ratingOverall}/5 · ${r.verifiedEligibilitySource}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall)
                                                Text(r.comment, style = MaterialTheme.typography.bodySmall)
                                                if (!r.gymReply.isNullOrBlank()) {
                                                    Text("Respuesta del gimnasio: ${r.gymReply}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        }
                                    }
                                }
                                if (canReview) {
                                    OutlinedTextField(
                                        value = reviewComment,
                                        onValueChange = { reviewComment = it },
                                        label = { Text("Comparte tu experiencia verificada (mín. 10 car.)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(onClick = {
                                        scope.launch {
                                            errorMsg = null
                                            saasApi.createGymReview(
                                                gym.id,
                                                CreateGymReviewRequest(
                                                    ratingOverall = reviewRating,
                                                    ratingCleanliness = reviewRating,
                                                    ratingEquipment = reviewRating,
                                                    ratingAttention = reviewRating,
                                                    ratingValue = reviewRating,
                                                    comment = reviewComment
                                                )
                                            ).onSuccess {
                                                reviewComment = ""
                                                noticeMsg = "Reseña verificada publicada correctamente."
                                                saasApi.listGymReviews(gym.id).onSuccess { if (activeReviewsGymId == gym.id) reviews = it }
                                            }.onFailure { errorMsg = it.message }
                                        }
                                    }) {
                                        Text("Publicar reseña")
                                    }
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
