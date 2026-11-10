package pe.edu.upeu.gymvip.presentation.screen.personal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pe.edu.upeu.gymvip.di.AppContainer
import pe.edu.upeu.gymvip.domain.model.*
import pe.edu.upeu.gymvip.presentation.component.*
import pe.edu.upeu.gymvip.presentation.navigation.AlVolver
import pe.edu.upeu.gymvip.presentation.viewmodel.*

@Composable
fun PersonalScreen(usuario: UsuarioPersonal, salir: () -> Unit) {
    val comercial = remember(usuario.id, usuario.roles) { ComunidadViewModel(AppContainer.comunidadRepository, false) }
    val preferencias = remember { PreferencesViewModel(AppContainer.configRepository) }
    DisposableEffect(comercial, preferencias) { onDispose { comercial.onCleared(); preferencias.onCleared() } }
    var verCatalogo by rememberSaveable(usuario.id) { mutableStateOf(false) }
    var verPreferencias by rememberSaveable(usuario.id) { mutableStateOf(false) }
    AlVolver(verCatalogo || verPreferencias) { verCatalogo = false; verPreferencias = false }
    val vm = remember(usuario.id, usuario.roles) { PersonalViewModel(AppContainer.gestionPersonalRepository) }
    DisposableEffect(vm) { onDispose { vm.onCleared() } }
    val state by vm.state.collectAsState()
    val secciones = SeccionPersonal.entries.filter { it != SeccionPersonal.CUENTAS || usuario.esAdministrador }
    var elegida by rememberSaveable(usuario.id) { mutableStateOf(SeccionPersonal.INICIO) }
    val seccion = elegida.takeIf { it in secciones } ?: SeccionPersonal.INICIO
    var busqueda by rememberSaveable(usuario.id) { mutableStateOf("") }
    var seleccionado by rememberSaveable(usuario.id) { mutableStateOf<String?>(null) }
    var editarSocio by remember { mutableStateOf<SocioGestion?>(null) }
    var nuevoSocio by remember { mutableStateOf(false) }
    var editarPlan by remember { mutableStateOf<PlanGestion?>(null) }
    var nuevoPlan by remember { mutableStateOf(false) }
    var cerrarSesion by remember { mutableStateOf(false) }
    var confirmarAsistencia by remember { mutableStateOf(false) }
    fun cargar() { if (seleccionado != null) vm.ficha(seleccionado!!) else vm.cargar(seccion, busqueda) }
    LaunchedEffect(seccion, seleccionado) { cargar() }
    AlVolver(!verCatalogo && !verPreferencias && (seleccionado != null || seccion != SeccionPersonal.INICIO)) {
        if (seleccionado != null) seleccionado = null else elegida = SeccionPersonal.INICIO
    }
    if (verCatalogo || verPreferencias) {
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            if (verCatalogo) pe.edu.upeu.gymvip.presentation.screen.catalog.CatalogoComunidadScreen(comercial) { verCatalogo = false }
            else pe.edu.upeu.gymvip.presentation.screen.preferences.PreferencesScreen(preferencias) { verPreferencias = false }
        }
        return
    }
    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    LogoGV()
                    Column(Modifier.weight(1f)) {
                        Text("GYM VIP · " + usuario.etiqueta, style = MaterialTheme.typography.titleMedium)
                        Text(usuario.correo, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton({ cargar() }, enabled = !state.cargando && !state.guardando) {
                        Icon(Icons.Outlined.Refresh, "Actualizar datos")
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                secciones.forEach { item ->
                    val icon = when (item) {
                        SeccionPersonal.INICIO -> Icons.Outlined.Dashboard
                        SeccionPersonal.SOCIOS -> Icons.Outlined.Groups
                        SeccionPersonal.PLANES -> Icons.Outlined.CreditCard
                        SeccionPersonal.CUENTAS -> Icons.Outlined.ManageAccounts
                        SeccionPersonal.PERFIL -> Icons.Outlined.PersonOutline
                    }
                    NavigationBarItem(selected = seccion == item, onClick = { seleccionado = null; elegida = item; vm.limpiarMensajes() },
                        icon = { Icon(icon, item.titulo) }, label = { Text(item.titulo, style = MaterialTheme.typography.labelSmall) })
                }
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                if (seleccionado != null) TextButton({ seleccionado = null }) { Text("‹  Volver a socios") }
                EncabezadoPantalla(if (seleccionado != null) "Ficha del socio" else when (seccion) {
                    SeccionPersonal.INICIO -> "Tu panel de trabajo"
                    else -> seccion.titulo
                }, if (seleccionado != null) "Información registrada por el gimnasio."
                else when (seccion) {
                    SeccionPersonal.INICIO -> "Atención y gestión de VIP Center Fit."
                    SeccionPersonal.SOCIOS -> "Busca por nombre, documento o código."
                    SeccionPersonal.PLANES -> if (usuario.esAdministrador) "Administra los planes del gimnasio." else "Consulta los planes para atender a los socios."
                    SeccionPersonal.CUENTAS -> "Cuentas del personal y sus permisos."
                    SeccionPersonal.PERFIL -> "Tu sesión de trabajo."
                }, usuario.etiqueta.uppercase())
            }
            state.mensaje?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
            state.error?.let { item { EstadoError(it) { cargar() } } }
            if (state.cargando) item { Esqueleto() }
            else if (seleccionado != null) {
                state.ficha?.let { ficha ->
                    item { Panel {
                        Text(ficha.socio.nombreCompleto, style = MaterialTheme.typography.headlineMedium)
                        Text(ficha.socio.codigo, color = MaterialTheme.colorScheme.primary)
                        FilaDetalle("Estado", ficha.socio.estado)
                        FilaDetalle(ficha.socio.tipoDocumento, ficha.socio.documento)
                        FilaDetalle("Nacimiento", ficha.socio.nacimiento)
                        FilaDetalle("Celular", ficha.socio.telefono.ifBlank { "Sin registrar" })
                        FilaDetalle("Correo", ficha.socio.correo.ifBlank { "Sin registrar" })
                        BotonSecundario("Editar datos", { nuevoSocio = false; editarSocio = ficha.socio; vm.limpiarMensajes() })
                    } }
                    item { Panel {
                        Text("Membresía", style = MaterialTheme.typography.titleLarge)
                        FilaDetalle("Plan", ficha.cobertura.plan.ifBlank { "Sin plan" })
                        FilaDetalle("Vigencia", ficha.cobertura.estado)
                        FilaDetalle("Último día válido", ficha.cobertura.ultimoDia.ifBlank { "—" })
                        Text(ficha.cobertura.motivo, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        BotonPrimario("Registrar asistencia", { confirmarAsistencia = true },
                            cargando = state.guardando, habilitado = ficha.cobertura.permiteAsistencia)
                    } }
                    item { Text("Pagos registrados", style = MaterialTheme.typography.titleLarge) }
                    if (ficha.pagos.isEmpty()) item { EstadoVacio("No hay pagos registrados para este socio.") }
                    ficha.pagos.forEach { pago -> item { Panel {
                        Text(MoneyPen.fromDouble(pago.importe).amountInSoles, color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge)
                        Text(pago.medio + " · " + pago.fecha)
                        if (pago.referencia.isNotBlank()) Text(pago.referencia)
                    } } }
                }
            } else when (seccion) {
                SeccionPersonal.INICIO -> {
                    state.resumen?.let { resumen ->
                        item { Panel {
                            Text("Socios registrados", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(resumen.socios.toString(), style = MaterialTheme.typography.headlineLarge)
                            TextButton({ elegida = SeccionPersonal.SOCIOS }) { Text("Consultar socios ›") }
                        } }
                        item { Panel {
                            Text("Vigencias con alerta", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(resumen.alertas.toString(), style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.primary)
                            Text("Consulta la ficha de cada socio para verificar su vigencia antes de registrar asistencia.")
                        } }
                        item { BotonPrimario("Registrar socio", { nuevoSocio = true; editarSocio = socioVacio(); vm.limpiarMensajes() }) }
                        item { BotonSecundario("Catálogo y promociones", { verCatalogo = true }) }
                    }
                }
                SeccionPersonal.SOCIOS -> {
                    item {
                        CampoTexto(busqueda, { busqueda = it }, "Nombre, documento o código")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TextButton({ vm.cargar(seccion, busqueda) }) { Text("Buscar") }
                            TextButton({ nuevoSocio = true; editarSocio = socioVacio(); vm.limpiarMensajes() }) { Text("+ Registrar socio") }
                        }
                    }
                    val page = state.pagina
                    if (page != null && page.socios.isEmpty()) item { EstadoVacio("No se encontraron socios con esa búsqueda.") }
                    page?.socios?.forEach { socio -> item(key = socio.id) { Panel({ seleccionado = socio.id }) {
                        Text(socio.nombreCompleto, style = MaterialTheme.typography.titleMedium)
                        Text(socio.codigo + " · " + socio.estado, color = MaterialTheme.colorScheme.primary)
                        Text(socio.tipoDocumento + " " + socio.documento, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } } }
                    if (page != null && page.totalPaginas > 1) item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton({ vm.cargar(seccion, busqueda, page.pagina - 1) }, enabled = page.pagina > 0) { Text("Anterior") }
                            Text("${page.pagina + 1} / ${page.totalPaginas}", Modifier.padding(top = 16.dp))
                            TextButton({ vm.cargar(seccion, busqueda, page.pagina + 1) }, enabled = page.pagina + 1 < page.totalPaginas) { Text("Siguiente") }
                        }
                    }
                }
                SeccionPersonal.PLANES -> {
                    if (usuario.esAdministrador) item { BotonPrimario("Crear plan", {
                        nuevoPlan = true; editarPlan = PlanGestion("", "", "", 30, 0.0, "", "", "ACTIVO"); vm.limpiarMensajes()
                    }) }
                    if (state.planes.isEmpty() && state.error == null) item { EstadoVacio("No hay planes registrados.") }
                    state.planes.forEach { plan -> item(key = plan.id) { Panel {
                        Text(plan.nombre, style = MaterialTheme.typography.titleLarge)
                        Text(MoneyPen.fromDouble(plan.precio).amountInSoles, color = MaterialTheme.colorScheme.primary)
                        Text("${plan.dias} días · ${plan.estado}")
                        if (plan.descripcion.isNotBlank()) Text(plan.descripcion)
                        if (plan.beneficios.isNotBlank()) Text("Beneficios: " + plan.beneficios)
                        if (plan.condiciones.isNotBlank()) Text("Condiciones: " + plan.condiciones)
                        if (usuario.esAdministrador) TextButton({ nuevoPlan = false; editarPlan = plan; vm.limpiarMensajes() }) { Text("Editar plan") }
                    } } }
                }
                SeccionPersonal.CUENTAS -> if (usuario.esAdministrador) {
                    if (state.cuentas.isEmpty() && state.error == null) item { EstadoVacio("No hay cuentas de personal para mostrar.") }
                    state.cuentas.forEach { cuenta -> item(key = cuenta.id) { Panel {
                        Text(cuenta.correo, style = MaterialTheme.typography.titleMedium)
                        Text(cuenta.roles.joinToString { it.etiqueta }, color = MaterialTheme.colorScheme.primary)
                        Text(cuenta.estado)
                    } } }
                }
                SeccionPersonal.PERFIL -> item { Panel {
                    Text(usuario.correo, style = MaterialTheme.typography.titleLarge)
                    FilaDetalle("Rol", usuario.etiqueta)
                    Text(if (usuario.esAdministrador) "Puedes gestionar socios y planes y consultar las cuentas del personal."
                        else "Puedes atender socios, revisar sus planes y registrar asistencias.")
                    BotonSecundario("Catálogo y promociones", { verCatalogo = true })
                    BotonSecundario("Preferencias", { verPreferencias = true })
                    BotonSecundario("Cerrar sesión", { cerrarSesion = true })
                } }
            }
        }
    }
    editarSocio?.let { socio -> FormularioSocio(socio, nuevoSocio, state, { if (!state.guardando) editarSocio = null }) { draft ->
        vm.guardarSocio(draft, nuevoSocio) { guardado -> editarSocio = null; elegida = SeccionPersonal.SOCIOS; seleccionado = guardado.id; vm.ficha(guardado.id) }
    } }
    if (usuario.esAdministrador) editarPlan?.let { plan -> FormularioPlan(plan, nuevoPlan, state,
        { if (!state.guardando) editarPlan = null }) { draft -> vm.guardarPlan(draft, nuevoPlan) {
            editarPlan = null; vm.cargar(SeccionPersonal.PLANES)
        } } }
    if (cerrarSesion) AlertDialog(onDismissRequest = { cerrarSesion = false }, title = { Text("Cerrar sesión") },
        text = { Text("Se cerrará tu sesión de trabajo en este dispositivo.") },
        confirmButton = { TextButton(salir) { Text("Cerrar sesión") } }, dismissButton = { TextButton({ cerrarSesion = false }) { Text("Cancelar") } })
    if (confirmarAsistencia) AlertDialog(onDismissRequest = { confirmarAsistencia = false }, title = { Text("Registrar asistencia") },
        text = { Text("Confirmar la asistencia de " + state.ficha?.socio?.nombreCompleto.orEmpty() + ". El servidor validará su vigencia.") },
        confirmButton = { TextButton({ confirmarAsistencia = false; seleccionado?.let(vm::asistencia) }) { Text("Confirmar") } },
        dismissButton = { TextButton({ confirmarAsistencia = false }) { Text("Cancelar") } })
}

private fun socioVacio() = SocioGestion("", "", "", "", "", "", "", "ACTIVO", "PE", "DNI", "")

@Composable
internal fun Panel(onClick: (() -> Unit)? = null, contenido: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = contenido)
    }
}
