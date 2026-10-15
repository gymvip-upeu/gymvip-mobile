package com.gymvip.mobile.presentation.screen.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.domain.model.ProductCategory
import com.gymvip.mobile.presentation.component.EmptyView
import com.gymvip.mobile.presentation.component.FavoriteItemCard
import com.gymvip.mobile.presentation.component.LoadingView
import com.gymvip.mobile.presentation.component.OfflineBanner
import com.gymvip.mobile.presentation.component.ProductItemCard
import com.gymvip.mobile.presentation.theme.GoldOnPrimary
import com.gymvip.mobile.presentation.theme.GoldPrimary
import com.gymvip.mobile.presentation.viewmodel.CatalogViewModel
import com.gymvip.mobile.presentation.viewmodel.FavoritesViewModel

@Composable
fun CatalogScreen(
    catalogViewModel: CatalogViewModel,
    favoritesViewModel: FavoritesViewModel
) {
    val catalogState by catalogViewModel.uiState.collectAsState()
    val favoritesState by favoritesViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        OfflineBanner(
            isOffline = catalogState.errorMessage != null,
            pendingSyncCount = favoritesState.pendingCount
        )

        TabRow(
            selectedTabIndex = catalogState.selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = GoldPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[catalogState.selectedSubTab]),
                    color = GoldPrimary
                )
            }
        ) {
            Tab(
                selected = catalogState.selectedSubTab == 0,
                onClick = { catalogViewModel.setSubTab(0) },
                text = { Text("Catálogo General", style = MaterialTheme.typography.titleSmall) }
            )
            Tab(
                selected = catalogState.selectedSubTab == 1,
                onClick = { catalogViewModel.setSubTab(1) },
                text = {
                    val count = favoritesState.favorites.size
                    Text("⭐ Mis Favoritos ($count)", style = MaterialTheme.typography.titleSmall)
                }
            )
        }

        when (catalogState.selectedSubTab) {
            0 -> CatalogTab(
                state = catalogState,
                onFilterCategory = { catalogViewModel.setCategoryFilter(it) },
                onAddToFavorites = { favoritesViewModel.openAddDialog(it) }
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
                        color = GoldPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = favoritesState.noteInput,
                        onValueChange = { favoritesViewModel.updateNoteInput(it) },
                        label = { Text("Nota personal (opcional)") },
                        placeholder = { Text("Ej: Consultar sabor vainilla en recepción") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            focusedLabelColor = GoldPrimary
                        )
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
                        containerColor = GoldPrimary,
                        contentColor = GoldOnPrimary
                    )
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { favoritesViewModel.dismissDialog() }) {
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
                        color = GoldPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = favoritesState.noteInput,
                        onValueChange = { favoritesViewModel.updateNoteInput(it) },
                        label = { Text("Nota personal") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            focusedLabelColor = GoldPrimary
                        )
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
                        containerColor = GoldPrimary,
                        contentColor = GoldOnPrimary
                    )
                ) {
                    Text("Actualizar Nota")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { favoritesViewModel.dismissDialog() }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun CatalogTab(
    state: com.gymvip.mobile.presentation.viewmodel.CatalogUiState,
    onFilterCategory: (ProductCategory?) -> Unit,
    onAddToFavorites: (com.gymvip.mobile.domain.model.Product) -> Unit
) {
    val scrollCategories = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollCategories)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.selectedCategory == null,
                onClick = { onFilterCategory(null) },
                label = { Text("Todos") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GoldPrimary,
                    selectedLabelColor = GoldOnPrimary
                )
            )
            ProductCategory.entries.forEach { cat ->
                val name = when (cat) {
                    ProductCategory.PLAN_MEMBRESIA -> "Planes"
                    ProductCategory.SUPLEMENTO -> "Suplementos"
                    ProductCategory.BEBIDA -> "Bebidas"
                    ProductCategory.MERCHANDISING -> "Merchandising"
                    ProductCategory.SERVICIO_ESPECIAL -> "Especiales"
                }
                FilterChip(
                    selected = state.selectedCategory == cat,
                    onClick = { onFilterCategory(cat) },
                    label = { Text(name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldPrimary,
                        selectedLabelColor = GoldOnPrimary
                    )
                )
            }
        }

        if (state.isLoading && state.products.isEmpty()) {
            LoadingView("Cargando catálogo...")
        } else if (state.products.isEmpty()) {
            EmptyView(
                icon = "📋",
                title = "Sin productos",
                message = "No se encontraron elementos en esta categoría."
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.products, key = { it.id }) { product ->
                    ProductItemCard(
                        product = product,
                        onAddToFavorites = onAddToFavorites
                    )
                }
            }
        }
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
                        color = GoldPrimary
                    )
                    OutlinedButton(onClick = onSyncNow) {
                        Text(text = if (state.isSyncing) "Sincronizando..." else "Sincronizar ahora")
                    }
                }
            }
        }

        if (state.favorites.isEmpty()) {
            EmptyView(
                icon = "⭐",
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
