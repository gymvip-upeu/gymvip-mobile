package pe.edu.upeu.gymvip.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LogoGV(modifier: Modifier = Modifier) {
    Box(modifier.size(36.dp).border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
        .semantics { contentDescription = "GYM VIP" }, contentAlignment = Alignment.Center) {
        Text("GV", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
fun BotonPrimario(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, cargando: Boolean = false, habilitado: Boolean = true) {
    Button(onClick, modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = habilitado && !cargando,
        shape = RoundedCornerShape(28.dp)) {
        if (cargando) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
        else Text(texto, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BotonSecundario(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick, modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)) {
        Text(texto, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CampoTexto(valor: String, onCambio: (String) -> Unit, etiqueta: String, modifier: Modifier = Modifier,
    esPassword: Boolean = false, error: String? = null,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
    habilitado: Boolean = true) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(valor, onCambio, modifier.fillMaxWidth(), label = { Text(etiqueta) },
        singleLine = true, enabled = habilitado, keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
        isError = error != null, shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface),
        visualTransformation = if (esPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (esPassword) {{ IconButton({ visible = !visible }) {
            Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                if (visible) "Ocultar contraseña" else "Mostrar contraseña")
        } }} else null,
        supportingText = if (error != null) {{ Text(error) }} else null)
}

@Composable
fun EncabezadoPantalla(titulo: String, subtitulo: String? = null, etiqueta: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        etiqueta?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp) }
        Text(titulo, style = MaterialTheme.typography.headlineLarge)
        subtitulo?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge) }
    }
}

@Composable
fun FilaDetalle(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(etiqueta, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(valor, Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

/** Puede recibir una imagen propia/licenciada de composeResources sin cambiar la tarjeta. */
@Composable
fun ImagenGimnasio(modifier: Modifier = Modifier, imagen: Painter? = null, descripcion: String = "Gimnasio GYM VIP") {
    Box(modifier.background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.surface, Color(0xFF514524))))) {
        if (imagen != null) androidx.compose.foundation.Image(imagen, descripcion, Modifier.matchParentSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop)
    }
}
