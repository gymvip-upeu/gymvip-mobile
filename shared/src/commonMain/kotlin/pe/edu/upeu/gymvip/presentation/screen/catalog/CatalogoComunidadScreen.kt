package pe.edu.upeu.gymvip.presentation.screen.catalog

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import pe.edu.upeu.gymvip.AppConfig
import pe.edu.upeu.gymvip.domain.model.*
import pe.edu.upeu.gymvip.presentation.component.*
import pe.edu.upeu.gymvip.presentation.navigation.AlVolver
import pe.edu.upeu.gymvip.presentation.viewmodel.ComunidadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarcoComunidad(titulo: String, subtitulo: String, volver: (() -> Unit)? = null,
    cargando: Boolean = false, refrescar: () -> Unit = {}, contenido: LazyListScope.() -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        PullToRefreshBox(cargando, refrescar, Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    volver?.let { TextButton(it) { Text("‹  Volver") } }
                    EncabezadoPantalla(titulo, subtitulo)
                }
                contenido()
            }
        }
    }
}

@Composable
fun ErrorCargaComunidad(mensaje: String, sinRed: Boolean, conDatos: Boolean, reintentar: () -> Unit) {
    if (sinRed && conDatos) Surface(color = androidx.compose.ui.graphics.Color(0xFF3C2E0C),
        contentColor = androidx.compose.ui.graphics.Color(0xFFFFD977), shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Sin conexión · Mostrando datos de esta sesión")
            Text(mensaje, style = MaterialTheme.typography.bodySmall)
            TextButton(reintentar, colors = ButtonDefaults.textButtonColors(contentColor = androidx.compose.ui.graphics.Color(0xFFFFD977))) { Text("Reintentar") }
        }
    } else EstadoError(mensaje, reintentar)
}

@Composable
fun CatalogoComunidadScreen(vm: ComunidadViewModel, volver: (() -> Unit)? = null) {
    val s by vm.state.collectAsState()
    var tab by rememberSaveable { mutableStateOf(0) }
    var query by rememberSaveable { mutableStateOf(s.filtro.consulta) }
    var filtros by remember { mutableStateOf(false) }
    var nota by remember { mutableStateOf<FavoritoSocio?>(null) }
    val favoritoHabilitado = vm.esSocio && AppConfig.FEATURE_FAVORITOS
    LaunchedEffect(tab) { when(tab) { 0 -> vm.cargarCatalogo(); 1 -> vm.cargarPromos(); else -> vm.cargarFavoritos() } }
    AlVolver(s.productoId != null || s.ofertaId != null) { vm.cerrarDetalle() }
    if (s.productoId != null || s.ofertaId != null) {
        DetalleComercial(vm) { producto -> tab = 1; vm.cerrarDetalle(); vm.cargarPromos(producto = producto) }
        return
    }
    MarcoComunidad("Catálogo", "Productos y promociones disponibles en recepción.", volver,
        when(tab) { 0 -> s.cargandoCatalogo; 1 -> s.cargandoPromos; else -> s.cargandoFavoritos },
        { when(tab) { 0 -> vm.cargarCatalogo(); 1 -> vm.cargarPromos(); else -> vm.cargarFavoritos() } }) {
        item {
            Row(Modifier.fillMaxWidth()) {
                listOf("Productos", "Promociones").plus(if (favoritoHabilitado) listOf("Favoritos") else emptyList()).forEachIndexed { i, label ->
                    Column(Modifier.weight(1f)) {
                        TextButton({ tab = i }, Modifier.fillMaxWidth()) {
                            Text(label, color = if (tab == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HorizontalDivider(thickness = if (tab == i) 2.dp else 1.dp,
                            color = if (tab == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    }
                }
            }
            HorizontalDivider()
        }
        if (tab == 0) {
            item {
                CampoTexto(query, { query = it }, "Buscar producto o marca")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton({ vm.cargarCatalogo(s.filtro.copy(consulta = query)) }) { Text("Buscar") }
                    TextButton({ filtros = true }) { Icon(Icons.Outlined.Tune, null); Text(" Filtros") }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("" to "Todos", "PROTEINA" to "Proteína", "CREATINA" to "Creatina", "PREENTRENO" to "Preentreno",
                        "AMINOACIDOS" to "Aminoácidos", "VITAMINAS" to "Vitaminas", "OTROS" to "Otros").forEach { (id, label) ->
                        ChipFiltro(label, s.filtro.categoria == id) { vm.cargarCatalogo(s.filtro.copy(categoria = id, consulta = query)) }
                    }
                }
            }
            s.errorCatalogo?.let { item { ErrorCargaComunidad(it, s.sinRedCatalogo, s.catalogo != null) { vm.cargarCatalogo() } } }
            s.errorPrecios?.let { item { EstadoError(it) { vm.cargarCatalogo() } } }
            if (s.cargandoCatalogo && s.catalogo == null) item { Esqueleto() }
            if (!s.cargandoCatalogo && s.errorCatalogo == null && s.catalogo?.contenido?.isEmpty() == true) item { EstadoVacio("No se encontraron productos con esos filtros.") }
            s.catalogo?.contenido?.chunked(2)?.forEach { fila -> item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                fila.forEach { producto -> ProductoCard(producto, vm, Modifier.weight(1f)) }
                if (fila.size == 1) Spacer(Modifier.weight(1f))
            } } }
            s.catalogo?.let { pagina -> item { Paginador(pagina.pagina, pagina.totalPaginas) { vm.cargarCatalogo(pagina = it) } } }
        } else if (tab == 1) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipFiltro("Vigentes", s.estadoPromos == "VIGENTE") { vm.cargarPromos("VIGENTE") }
                    ChipFiltro("Finalizadas", s.estadoPromos == "FINALIZADA") { vm.cargarPromos("FINALIZADA") }
                }
                if (s.productoPromo != null) TextButton({ vm.cargarPromos(producto = null) }) { Text("Ver todos los productos") }
                if (vm.esSocio && s.estadoPromos == "FINALIZADA") Text("El servidor publica únicamente promociones vigentes para socios.", style = MaterialTheme.typography.bodyMedium)
            }
            s.errorPromos?.let { item { ErrorCargaComunidad(it, s.sinRedPromos, s.promociones != null) { vm.cargarPromos() } } }
            if (s.cargandoPromos && s.promociones == null) item { Esqueleto() }
            if (!s.cargandoPromos && s.errorPromos == null && s.promociones?.contenido?.isEmpty() == true) item { EstadoVacio("No hay promociones para esta consulta.") }
            s.promociones?.contenido?.forEach { oferta -> item(key = oferta.id) { OfertaCard(oferta, vm) { vm.abrirOferta(oferta.id) } } }
            s.promociones?.let { pagina -> item { Paginador(pagina.pagina, pagina.totalPaginas) { vm.cargarPromos(pagina = it) } } }
        } else if (favoritoHabilitado) {
            s.errorFavoritos?.let { item { ErrorCargaComunidad(it, s.sinRedFavoritos, s.datos.favoritos.isNotEmpty(), vm::cargarFavoritos) } }
            if (s.cargandoFavoritos && s.datos.favoritos.isEmpty()) item { Esqueleto() }
            if (!s.cargandoFavoritos && s.errorFavoritos == null && s.datos.favoritos.isEmpty()) item { EstadoVacio("Aún no guardaste favoritos. Toca el corazón de un producto para añadirlo.") }
            s.datos.favoritos.forEach { favorito -> item(key = favorito.id) { PanelSocio {
                Text(favorito.nombre, style = MaterialTheme.typography.titleMedium)
                Text(favorito.categoria, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(favorito.nota.ifBlank { "Sin nota personal" })
                Row { TextButton({ vm.abrirProducto(favorito.productoId) }) { Text("Ver producto") }
                    TextButton({ nota = favorito }) { Text("Editar nota") }
                    TextButton({ vm.quitar(favorito.id) }, enabled = !s.guardando) { Text("Quitar") } }
            } } }
        }
    }
    if (filtros) FiltrosDialog(s.filtro, { filtros = false }) { filtros = false; vm.cargarCatalogo(it.copy(consulta = query)) }
    nota?.let { favorito ->
        var texto by remember(favorito.id) { mutableStateOf(favorito.nota) }
        AlertDialog(onDismissRequest = { if (!s.guardando) nota = null }, title = { Text("Mi nota personal") },
            text = { Column { OutlinedTextField(texto, { texto = it }, label = { Text("Nota") }, isError = texto.length > 250,
                supportingText = { Text("${texto.length}/250") }); s.errorAccion?.let { Text(it, color = MaterialTheme.colorScheme.error) } } },
            confirmButton = { TextButton({ vm.nota(favorito.id, texto) { nota = null } }, enabled = !s.guardando && texto.length <= 250) { Text("Guardar") } },
            dismissButton = { TextButton({ nota = null }, enabled = !s.guardando) { Text("Cancelar") } })
    }
}

@Composable
private fun ProductoCard(p: Suplemento, vm: ComunidadViewModel, modifier: Modifier) {
    val s by vm.state.collectAsState()
    Card(modifier.clickable { vm.abrirProducto(p.id) }, shape = MaterialTheme.shapes.medium) {
        Box { FotoProducto(p, vm, Modifier.fillMaxWidth().height(130.dp))
            Surface(Modifier.align(Alignment.BottomStart).padding(8.dp), shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.background.copy(alpha = .85f)) {
                Text(p.categoria.lowercase().replaceFirstChar(Char::uppercaseChar), Modifier.padding(6.dp), style = MaterialTheme.typography.labelSmall)
            }
            if (vm.esSocio && AppConfig.FEATURE_FAVORITOS) IconButton({ vm.alternarFavorito(p) }, Modifier.align(Alignment.TopEnd), enabled = !s.guardando) {
                Icon(if (s.datos.favoritos.any { it.productoId == p.id }) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                    if (s.datos.favoritos.any { it.productoId == p.id }) "Quitar ${p.nombre} de favoritos" else "Guardar ${p.nombre} en favoritos",
                    tint = MaterialTheme.colorScheme.primary)
            }
        }
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(p.nombre, style = MaterialTheme.typography.titleSmall)
            Text(p.marca, style = MaterialTheme.typography.labelSmall)
            val oferta = s.ofertasEnCatalogo[p.id]
            if (oferta != null) Text(oferta.precioReferencia.amountInSoles, style = MaterialTheme.typography.labelSmall,
                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
            Text((oferta?.precioOferta ?: p.precio).amountInSoles, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            Text(if (oferta != null) "Promoción vigente" else "Precio regular", style = MaterialTheme.typography.labelSmall)
            Text(if (p.disponible) "Disponible en recepción" else "Agotado temporalmente", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun FotoProducto(p: Suplemento, vm: ComunidadViewModel, modifier: Modifier = Modifier) {
    var bitmap by remember(p.id, p.imagenes) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(p.id, p.imagenes) { p.imagenes.firstOrNull()?.let { id ->
        val bytes = vm.repository.imagen(p.id, id).getOrNull()
        bitmap = bytes?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() }
    } }
    Box(modifier) {
        ImagenGimnasio(Modifier.matchParentSize())
        bitmap?.let { Image(it, "Imagen de ${p.nombre}", Modifier.matchParentSize(), contentScale = ContentScale.Crop) }
        if (bitmap == null) Icon(Icons.Outlined.FitnessCenter, "${p.nombre}: imagen no disponible", Modifier.align(Alignment.Center).size(36.dp), tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun OfertaCard(o: Oferta, vm: ComunidadViewModel? = null, click: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClickLabel = "Ver condiciones de ${o.nombre}", onClick = click),
        shape = MaterialTheme.shapes.large, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        if (vm != null) Box(Modifier.fillMaxWidth().height(148.dp)) {
            FotoProducto(o.producto, vm, Modifier.matchParentSize())
            Box(Modifier.matchParentSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(
                androidx.compose.ui.graphics.Color.Transparent, androidx.compose.ui.graphics.Color.Black.copy(alpha = .9f)))))
            Text(o.nombre, Modifier.align(Alignment.BottomStart).padding(16.dp), style = MaterialTheme.typography.titleLarge,
                color = androidx.compose.ui.graphics.Color.White)
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (o.vigente) "Hasta " + fechaCortaSocio(o.fin) else "Promoción ${o.estado.lowercase()}", color = MaterialTheme.colorScheme.primary)
            if (vm == null) Text(o.nombre, style = MaterialTheme.typography.titleLarge)
            Text(o.producto.nombre)
            Text(o.precioOferta.amountInSoles, color = MaterialTheme.colorScheme.primary)
            if (!o.producto.disponible) Text("Producto agotado temporalmente")
        }
    }
}

@Composable
private fun Paginador(pagina: Int, total: Int, ir: (Int) -> Unit) {
    if (total < 2) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton({ ir(pagina - 1) }, enabled = pagina > 0) { Text("Anterior") }
        Text("${pagina + 1} / $total", Modifier.padding(top = 14.dp))
        TextButton({ ir(pagina + 1) }, enabled = pagina + 1 < total) { Text("Siguiente") }
    }
}

@Composable
private fun FiltrosDialog(inicial: FiltroCatalogo, cerrar: () -> Unit, aplicar: (FiltroCatalogo) -> Unit) {
    var f by remember { mutableStateOf(inicial) }
    AlertDialog(onDismissRequest = cerrar, title = { Text("Filtrar catálogo") }, text = { Column {
        CampoTexto(f.minimo, { f = f.copy(minimo = it.replace(',', '.')) }, "Precio mínimo")
        CampoTexto(f.maximo, { f = f.copy(maximo = it.replace(',', '.')) }, "Precio máximo")
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(f.enPromocion, { f = f.copy(enPromocion = it) }); Text("En promoción") }
        Row { FilterChip(f.disponibilidad == "", { f = f.copy(disponibilidad = "") }, label = { Text("Todos") })
            FilterChip(f.disponibilidad == "DISPONIBLE", { f = f.copy(disponibilidad = "DISPONIBLE") }, label = { Text("Disponibles") }) }
    } }, confirmButton = { TextButton({ aplicar(f) }) { Text("Aplicar") } }, dismissButton = { TextButton(cerrar) { Text("Cancelar") } })
}
