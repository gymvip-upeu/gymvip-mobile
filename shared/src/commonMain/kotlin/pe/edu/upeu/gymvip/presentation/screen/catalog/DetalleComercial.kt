package pe.edu.upeu.gymvip.presentation.screen.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.ktor.http.encodeURLParameter
import pe.edu.upeu.gymvip.AppConfig
import pe.edu.upeu.gymvip.domain.model.*
import pe.edu.upeu.gymvip.presentation.component.*
import pe.edu.upeu.gymvip.presentation.viewmodel.ComunidadViewModel

@Composable
fun DetalleComercial(vm: ComunidadViewModel, verPromociones: (String) -> Unit) {
    val s by vm.state.collectAsState()
    val producto = s.producto ?: s.oferta?.producto
    MarcoComunidad(if (s.ofertaId != null) "Promoción" else "Detalle del producto", "Consulta la información registrada por el gimnasio.", vm::cerrarDetalle) {
        if (s.cargandoDetalle) item { Esqueleto() }
        s.errorDetalle?.let { item { EstadoError(it) { s.productoId?.let(vm::abrirProducto) ?: s.ofertaId?.let(vm::abrirOferta) } } }
        producto?.let { p ->
            item { FotoProducto(p, vm, Modifier.fillMaxWidth().height(240.dp)) }
            item { EncabezadoPantalla(s.oferta?.nombre ?: p.nombre, p.marca, p.categoria) }
            item { PanelSocio {
                if (s.oferta != null) {
                    val o = s.oferta!!
                    Text(if (o.vigente) "Promoción vigente" else "Promoción ${o.estado.lowercase()}", color = MaterialTheme.colorScheme.primary)
                    Text(o.precioReferencia.amountInSoles, textDecoration = TextDecoration.LineThrough)
                    Text(o.precioOferta.amountInSoles, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Text(o.descripcion)
                    FilaDetalle("Inicio", fechaCortaSocio(o.inicio)); FilaDetalle("Fin", fechaCortaSocio(o.fin))
                    Text("Condiciones", style = MaterialTheme.typography.titleMedium); Text(o.condiciones)
                } else {
                    s.ofertaProducto?.let { o ->
                        Text(p.precio.amountInSoles, textDecoration = TextDecoration.LineThrough)
                        Text("Oferta vigente: " + o.precioOferta.amountInSoles, color = MaterialTheme.colorScheme.primary)
                        TextButton({ vm.abrirOferta(o.id) }) { Text("Ver condiciones de la promoción") }
                    } ?: Text(p.precio.amountInSoles, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                }
                Text(if (p.disponible) "Disponible en recepción" else "Agotado temporalmente")
                FilaDetalle("Presentación", p.presentacion); FilaDetalle("Contenido neto", p.contenido)
                if (p.sabor.isNotBlank()) FilaDetalle("Sabor", p.sabor)
                Text(p.descripcion)
                if (p.ingredientes.isNotBlank()) { Text("Ingredientes", style = MaterialTheme.typography.titleMedium); Text(p.ingredientes) }
                TextButton({ verPromociones(p.id) }) { Text("Ver promociones de este producto") }
                if (vm.esSocio && AppConfig.FEATURE_FAVORITOS) BotonSecundario(
                    if (s.datos.favoritos.any { it.productoId == p.id }) "Quitar de favoritos" else "Guardar en favoritos", { vm.alternarFavorito(p) })
                ContactoWhatsApp("Hola, quiero información sobre ${p.nombre} (${p.presentacion}).")
            } }
        }
    }
}

@Composable
fun ContactoWhatsApp(mensaje: String, texto: String = "Consultar por WhatsApp") {
    val uri = LocalUriHandler.current
    if (AppConfig.WHATSAPP_GYM.isNotBlank()) BotonPrimario(texto, {
        uri.openUri("https://wa.me/" + AppConfig.WHATSAPP_GYM.filter(Char::isDigit) + "?text=" + mensaje.encodeURLParameter())
    }) else Text("Para más información, consulta en recepción.", color = MaterialTheme.colorScheme.onSurfaceVariant)
}
