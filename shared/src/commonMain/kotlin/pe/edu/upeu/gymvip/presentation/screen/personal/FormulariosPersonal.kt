package pe.edu.upeu.gymvip.presentation.screen.personal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.edu.upeu.gymvip.domain.model.*
import pe.edu.upeu.gymvip.presentation.component.CampoTexto
import pe.edu.upeu.gymvip.presentation.viewmodel.PersonalUiState

@Composable
internal fun FormularioSocio(inicial: SocioGestion, nuevo: Boolean, state: PersonalUiState,
    cerrar: () -> Unit, guardar: (SocioGestion) -> Unit) {
    var draft by remember(inicial) { mutableStateOf(inicial) }
    AlertDialog(onDismissRequest = cerrar, title = { Text(if (nuevo) "Registrar socio" else "Editar socio") },
        text = { Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CampoTexto(draft.nombres, { draft = draft.copy(nombres = it) }, "Nombres", habilitado = !state.guardando)
            CampoTexto(draft.apellidos, { draft = draft.copy(apellidos = it) }, "Apellidos", habilitado = !state.guardando)
            if (nuevo) CampoTexto(draft.nacimiento, { draft = draft.copy(nacimiento = it) }, "Nacimiento (AAAA-MM-DD)", habilitado = !state.guardando)
            Text("Documento", style = MaterialTheme.typography.labelLarge)
            listOf("DNI", "CARNET_EXTRANJERIA", "PASAPORTE").forEach { tipo ->
                FilterChip(draft.tipoDocumento == tipo, { draft = draft.copy(tipoDocumento = tipo) },
                    label = { Text(tipo.replace('_', ' ')) }, enabled = !state.guardando)
            }
            CampoTexto(draft.pais, { draft = draft.copy(pais = it.uppercase()) }, "País (PE, ...)", habilitado = !state.guardando)
            CampoTexto(draft.documento, { draft = draft.copy(documento = it) }, "Número de documento", habilitado = !state.guardando)
            CampoTexto(draft.telefono, { draft = draft.copy(telefono = it) }, "Celular (opcional)", habilitado = !state.guardando)
            CampoTexto(draft.correo, { draft = draft.copy(correo = it) }, "Correo de contacto (opcional)", habilitado = !state.guardando)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.guardando) LinearProgressIndicator(Modifier.fillMaxWidth())
        } },
        confirmButton = { TextButton({ guardar(draft) }, enabled = !state.guardando) { Text("Guardar") } },
        dismissButton = { TextButton(cerrar, enabled = !state.guardando) { Text("Cancelar") } })
}

@Composable
internal fun FormularioPlan(inicial: PlanGestion, nuevo: Boolean, state: PersonalUiState,
    cerrar: () -> Unit, guardar: (PlanGestion) -> Unit) {
    var draft by remember(inicial) { mutableStateOf(inicial) }
    var dias by remember(inicial) { mutableStateOf(inicial.dias.toString()) }
    var precio by remember(inicial) { mutableStateOf(if (inicial.precio == 0.0) "" else inicial.precio.toString()) }
    var errorLocal by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = cerrar, title = { Text(if (nuevo) "Crear plan" else "Editar plan") },
        text = { Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CampoTexto(draft.nombre, { draft = draft.copy(nombre = it) }, "Nombre", habilitado = !state.guardando)
            CampoTexto(draft.descripcion, { draft = draft.copy(descripcion = it) }, "Descripción", habilitado = !state.guardando)
            CampoTexto(dias, { dias = it }, "Duración en días", habilitado = !state.guardando)
            CampoTexto(precio, { precio = it }, "Precio en soles", habilitado = !state.guardando)
            CampoTexto(draft.beneficios, { draft = draft.copy(beneficios = it) }, "Beneficios", habilitado = !state.guardando)
            CampoTexto(draft.condiciones, { draft = draft.copy(condiciones = it) }, "Condiciones", habilitado = !state.guardando)
            (errorLocal ?: state.error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.guardando) LinearProgressIndicator(Modifier.fillMaxWidth())
        } },
        confirmButton = { TextButton({
            val monto = precio.replace(',', '.')
            if (!monto.matches(Regex("[0-9]+(\\.[0-9]{1,2})?")) || dias.toIntOrNull() == null)
                errorLocal = "Ingresa días enteros y un precio con hasta dos decimales."
            else { errorLocal = null; guardar(draft.copy(dias = dias.toInt(), precio = monto.toDoubleOrNull() ?: 0.0)) }
        }, enabled = !state.guardando) { Text("Guardar") } },
        dismissButton = { TextButton(cerrar, enabled = !state.guardando) { Text("Cancelar") } })
}
