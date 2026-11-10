package pe.edu.upeu.gymvip.presentation.screen.faq

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.edu.upeu.gymvip.presentation.component.*
import pe.edu.upeu.gymvip.presentation.screen.catalog.*
import pe.edu.upeu.gymvip.presentation.viewmodel.ComunidadViewModel

@Composable
fun AyudaComunidadScreen(vm: ComunidadViewModel, volver: () -> Unit) {
    val s by vm.state.collectAsState()
    var busqueda by rememberSaveable { mutableStateOf("") }
    var productos by rememberSaveable { mutableStateOf(false) }
    var abierta by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { vm.cargarFaq() }
    val preguntas = s.datos.preguntas.filter {
        val categoria = it.category.lowercase().replace('á', 'a')
        val sobreProducto = listOf("suplement", "producto", "catalog").any(categoria::contains)
        productos == sobreProducto && (busqueda.isBlank() || (it.question + " " + it.answer).contains(busqueda, ignoreCase = true))
    }
    MarcoComunidad("Ayuda y FAQ", "Respuestas publicadas por el gimnasio.", volver, s.cargandoFaq, vm::cargarFaq) {
        item {
            CampoTexto(busqueda, { busqueda = it }, "Buscar por palabras clave")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipFiltro("Gimnasio", !productos) { productos = false }
                ChipFiltro("Productos", productos) { productos = true }
            }
        }
        s.errorFaq?.let { item { ErrorCargaComunidad(it, s.sinRedFaq, s.datos.preguntas.isNotEmpty(), vm::cargarFaq) } }
        if (s.cargandoFaq && s.datos.preguntas.isEmpty()) item { Esqueleto() }
        if (!s.cargandoFaq && s.errorFaq == null && preguntas.isEmpty()) item { EstadoVacio("No se encontraron preguntas para esta consulta.") }
        preguntas.forEach { faq -> item(key = faq.id) { PanelSocio {
            TextButton({ abierta = if (abierta == faq.id) null else faq.id }) { Text(faq.question) }
            if (abierta == faq.id) Text(faq.answer)
        } } }
        item { Text("¿No encontraste tu respuesta?", style = MaterialTheme.typography.titleMedium)
            ContactoWhatsApp("Hola, tengo una consulta para recepción.", "Escribir a recepción por WhatsApp") }
    }
}
