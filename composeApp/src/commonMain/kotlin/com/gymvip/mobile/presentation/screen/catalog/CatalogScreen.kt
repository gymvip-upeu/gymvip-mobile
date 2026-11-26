package com.gymvip.mobile.presentation.screen.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.domain.model.Product
import com.gymvip.mobile.presentation.component.ContextualHelpCard
import kotlinx.coroutines.CancellationException
import com.gymvip.mobile.domain.model.ProductCategory
import com.gymvip.mobile.presentation.component.EmptyView
import com.gymvip.mobile.presentation.component.FavoriteItemCard
import com.gymvip.mobile.presentation.component.LoadingView
import com.gymvip.mobile.presentation.component.OfflineBanner
import com.gymvip.mobile.presentation.viewmodel.CatalogViewModel
import com.gymvip.mobile.presentation.viewmodel.FavoritesViewModel
import com.gymvip.mobile.presentation.theme.accentForeground
import com.gymvip.mobile.presentation.theme.vipOutlinedTextFieldColors
import com.gymvip.mobile.presentation.theme.VipTypography

@Composable
fun CatalogScreen(
    catalogViewModel: CatalogViewModel,
    favoritesViewModel: FavoritesViewModel,
    receptionWhatsAppNumber: String?,
    canPurchase: Boolean = false,
    cartCount: Int = 0,
    cartError: String? = null,
    isRefreshingCart: Boolean = false,
    onRetryCart: () -> Unit = {},
    onOpenCart: () -> Unit = {},
    onAddToCart: (Product) -> Unit = {}
) {
    val catalogState by catalogViewModel.uiState.collectAsState()
    val favoritesState by favoritesViewModel.uiState.collectAsState()
    LaunchedEffect(catalogViewModel) { catalogViewModel.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        OfflineBanner(
            isOffline = false,
            pendingSyncCount = favoritesState.pendingCount
        )
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("Suplementos", style = MaterialTheme.typography.headlineMedium)
            Text("Explora productos y promociones disponibles en recepción.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (canPurchase) {
            OutlinedButton(onClick = onOpenCart, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Icon(Icons.Outlined.ShoppingCart, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Mi carrito · $cartCount ${if (cartCount == 1) "producto" else "productos"} · Pedidos")
            }
        }
        if (cartError != null) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("No se pudo actualizar el carrito o tus pedidos. Puedes reintentar.", color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = onRetryCart, enabled = !isRefreshingCart) {
                    Text(if (isRefreshingCart) "Actualizando…" else "Reintentar carrito")
                }
            }
        }

        ContextualHelpCard(
            screenKey = "catalog_help",
            title = "Catálogo y Suplementos",
            description = "Explore suplementos, promociones vigentes y planes. Puede guardar favoritos para consultarlos sin conexión o contactar a recepción por WhatsApp."
        )

        TabRow(
            selectedTabIndex = catalogState.selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.accentForeground,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[catalogState.selectedSubTab]),
                    color = MaterialTheme.colorScheme.accentForeground
                )
            }
        ) {
            Tab(
                selected = catalogState.selectedSubTab == 0,
                onClick = { catalogViewModel.setSubTab(0) },
                text = { Text("Productos", style = MaterialTheme.typography.titleSmall) }
            )
            Tab(
                selected = catalogState.selectedSubTab == 1,
                onClick = { catalogViewModel.setSubTab(1) },
                text = {
                    val count = favoritesState.favorites.size
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Outlined.StarBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Favoritos ($count)", style = MaterialTheme.typography.titleSmall)
                    }
                }
            )
        }

        when (catalogState.selectedSubTab) {
            0 -> CatalogTab(
                state = catalogState,
                favoriteProductIds = favoritesState.favorites.filter { it.syncStatus != com.gymvip.mobile.domain.model.SyncStatus.PENDING_DELETE }.map { it.productId }.toSet(),
                onFilterCategory = { catalogViewModel.setCategoryFilter(it) },
                onFilterPromotions = catalogViewModel::togglePromotionsOnly,
                onRefresh = catalogViewModel::refresh,
                loadThumbnail = catalogViewModel::getThumbnail,
                receptionWhatsAppNumber = receptionWhatsAppNumber,
                onAddToFavorites = { favoritesViewModel.openAddDialog(it) },
                onAddToCart = if (canPurchase && !catalogState.isStale && !catalogState.isLoading) onAddToCart else null
            )
            1 -> FavoritesTab(
                state = favoritesState,
                onEditNote = { favoritesViewModel.openEditNoteDialog(it) },
                onDelete = { favoritesViewModel.deleteFavorite(it) },
                onSyncNow = { favoritesViewModel.syncPending() }
            )
        }
    }

    // Diálogo para Agregar Favorito con Nota Personal
    if (favoritesState.isAddingDialogForProduct != null) {
        val product = favoritesState.isAddingDialogForProduct
        AlertDialog(
            onDismissRequest = { favoritesViewModel.dismissDialog() },
            title = {
                Text(
                    text = "Guardar en Mis Favoritos",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column {
                    Text(
                        text = "Producto: ${product?.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.accentForeground
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = favoritesState.noteInput,
                        onValueChange = { favoritesViewModel.updateNoteInput(it) },
                        label = { Text("Nota personal (opcional)") },
                        placeholder = { Text("Ej: Consultar sabor vainilla en recepción") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = vipOutlinedTextFieldColors()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${favoritesState.noteInput.length} / 250 caracteres",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { favoritesViewModel.saveFavoriteFromDialog() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { favoritesViewModel.dismissDialog() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.accentForeground)
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo para Editar Nota Personal de Favorito Existente
    if (favoritesState.activeDialogFavorite != null) {
        val fav = favoritesState.activeDialogFavorite
        AlertDialog(
            onDismissRequest = { favoritesViewModel.dismissDialog() },
            title = { Text("Editar Nota Personal") },
            text = {
                Column {
                    Text(
                        text = "Producto: ${fav?.productName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.accentForeground
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = favoritesState.noteInput,
                        onValueChange = { favoritesViewModel.updateNoteInput(it) },
                        label = { Text("Nota personal") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = vipOutlinedTextFieldColors()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${favoritesState.noteInput.length} / 250 caracteres",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { favoritesViewModel.saveEditedNote() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Actualizar Nota")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { favoritesViewModel.dismissDialog() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.accentForeground)
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun CatalogTab(
    state: com.gymvip.mobile.presentation.viewmodel.CatalogUiState,
    favoriteProductIds: Set<String>,
    onFilterCategory: (ProductCategory?) -> Unit,
    onFilterPromotions: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    loadThumbnail: suspend (String) -> ByteArray,
    receptionWhatsAppNumber: String?,
    onAddToFavorites: (Product) -> Unit,
    onAddToCart: ((Product) -> Unit)?
) {
    val scrollCategories = rememberScrollState()
    var query by remember { mutableStateOf("") }
    var maxPrice by remember { mutableStateOf("") }
    var supplementCategory by remember { mutableStateOf<String?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var finishedOnly by remember { mutableStateOf(false) }
    val categories = state.products.mapNotNull { it.catalogCategory }
        .filter { code -> ProductCategory.entries.none { it.name == code } }.distinct().sorted()
    val limit = maxPrice.trim().replace(',', '.').toDoubleOrNull()
        ?.takeIf { it.isFinite() && it >= 0 }
    val badLimit = maxPrice.isNotBlank() && limit == null
    val visibleProducts = if (badLimit) emptyList() else state.products.filterCatalog(
        query, state.selectedCategory, supplementCategory, state.onlyPromotions && !finishedOnly, limit, finishedOnly
    )

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(value = query, onValueChange = { query = it },
            label = { Text("Buscar producto o marca") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        OutlinedTextField(value = maxPrice, onValueChange = { maxPrice = it },
            label = { Text("Precio máximo (S/)") }, singleLine = true, isError = badLimit,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollCategories)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !state.onlyPromotions && !finishedOnly,
                onClick = { finishedOnly = false; onFilterPromotions(false) },
                label = { Text("Todos") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
            FilterChip(
                selected = state.onlyPromotions && !finishedOnly,
                onClick = { finishedOnly = false; onFilterPromotions(true) },
                label = { Text("Promociones") }
            )
            FilterChip(
                selected = finishedOnly,
                onClick = { finishedOnly = true; onFilterPromotions(false) },
                label = { Text("Finalizadas") }
            )
            FilterChip(
                selected = state.selectedCategory == null && supplementCategory == null,
                onClick = { supplementCategory = null; onFilterCategory(null) },
                label = { Text("Todas las categorías") }
            )
            ProductCategory.entries.filter { category ->
                state.products.any { it.category == category }
            }.forEach { cat ->
                val name = when (cat) {
                    ProductCategory.PLAN_MEMBRESIA -> "Planes"
                    ProductCategory.SUPLEMENTO -> "Suplementos"
                    ProductCategory.BEBIDA -> "Bebidas"
                    ProductCategory.MERCHANDISING -> "Merchandising"
                    ProductCategory.SERVICIO_ESPECIAL -> "Especiales"
                }
                FilterChip(
                    selected = state.selectedCategory == cat && supplementCategory == null,
                    onClick = { supplementCategory = null; onFilterCategory(cat) },
                    label = { Text(name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
            categories.forEach { category ->
                FilterChip(
                    selected = supplementCategory == category,
                    onClick = { supplementCategory = category; onFilterCategory(null) },
                    label = { Text(category.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        if (state.isLoading) {
            androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(if (state.hasLoaded) "Actualizando catálogo… Mostrando información anterior hasta completar la consulta." else "Actualizando catálogo…",
                modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall)
        }
        if (state.errorMessage != null) {
            Text(if (state.isStale) "No se pudo actualizar. Información anterior: el precio y la disponibilidad pueden haber cambiado." else state.errorMessage,
                modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = onRefresh, enabled = !state.isLoading, modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Reintentar")
            }
        }
        if (state.isLoading && state.products.isEmpty()) {
            LoadingView("Cargando catálogo...")
        } else if (visibleProducts.isEmpty() && !badLimit && state.errorMessage == null) {
            EmptyView(
                icon = Icons.Outlined.ShoppingBag,
                title = if (state.onlyPromotions || finishedOnly) "Sin promociones" else "Sin productos",
                message = "No se encontraron elementos en esta categoría."
            )
        } else {
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val textScale = MaterialTheme.typography.bodyLarge.fontSize.value / VipTypography.bodyLarge.fontSize.value
                val columns = catalogColumnCount(maxWidth.value, LocalDensity.current.fontScale * textScale)
                LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    visibleProducts.chunked(columns).forEach { row ->
                        item(key = row.joinToString("-") { it.id }) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { product ->
                                    CatalogShopCard(product, finishedOnly, { selectedId = product.id },
                                        onAddToFavorites, onAddToCart,
                                        { CatalogImage(product, loadThumbnail, Modifier.fillMaxSize()) },
                                        Modifier.weight(1f), isFavorite = product.id in favoriteProductIds, compact = columns == 2)
                                }
                                if (columns == 2 && row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
    state.products.firstOrNull { it.id == selectedId }?.let { product ->
        CatalogDetailSheet(product, finishedOnly, receptionWhatsAppNumber,
            onDismiss = { selectedId = null }, onAddToCart = onAddToCart,
            catalogNotice = if (state.isStale || state.isLoading) "Información anterior. Actualiza el catálogo para comprobar el precio y la disponibilidad antes de agregar al carrito." else null,
            image = { CatalogImage(product, loadThumbnail, Modifier.fillMaxWidth().height(240.dp)) })
    }
}

@Composable
private fun CatalogImage(product: Product, loadThumbnail: suspend (String) -> ByteArray, modifier: Modifier = Modifier.fillMaxWidth().height(184.dp)) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, product.imageUrl, product.id) {
        value = null
        val path = product.imageUrl ?: return@produceState
        try {
            value = decodeCatalogThumbnail(loadThumbnail(path))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // The product details remain available if the thumbnail fails.
        }
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center) {
        bitmap?.let {
            androidx.compose.foundation.Image(bitmap = it, contentDescription = product.name,
                modifier = Modifier.fillMaxSize().padding(14.dp), contentScale = ContentScale.Fit)
        } ?: Icon(Icons.Outlined.ShoppingBag, contentDescription = "Imagen no disponible de ${product.name}",
            modifier = Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun FavoritesTab(
    state: com.gymvip.mobile.presentation.viewmodel.FavoritesUiState,
    onEditNote: (com.gymvip.mobile.domain.model.Favorite) -> Unit,
    onDelete: (com.gymvip.mobile.domain.model.Favorite) -> Unit,
    onSyncNow: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (state.pendingCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pendientes en cola: ${state.pendingCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.accentForeground
                    )
                    OutlinedButton(
                        onClick = onSyncNow,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.accentForeground)
                    ) {
                        Text(text = if (state.isSyncing) "Sincronizando..." else "Sincronizar ahora")
                    }
                }
            }
        }

        if (state.favorites.isEmpty()) {
            EmptyView(
                icon = Icons.Outlined.StarBorder,
                title = "Sin favoritos aún",
                message = "Explore el catálogo y guarde productos con notas personales para consultarlos offline."
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.favorites, key = { it.id }) { favorite ->
                    FavoriteItemCard(
                        favorite = favorite,
                        onEditNote = onEditNote,
                        onDelete = onDelete
                    )
                }
            }
        }
    }
}
