package pe.edu.upeu.gymvip.presentation.screen.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.edu.upeu.gymvip.presentation.component.LogoGV

@Composable
fun MarcoAcceso(onVolver: () -> Unit, contenido: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LogoGV()
                Text("GYM VIP", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("ACCESO GYM VIP", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(40.dp))
            TextButton(onVolver, contentPadding = PaddingValues(horizontal = 0.dp)) { Text("‹  Volver") }
            Spacer(Modifier.height(20.dp))
            contenido()
        }
    }
}

@Composable
fun AvisoAcceso(mensaje: String) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
        Text(mensaje, Modifier.fillMaxWidth().padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun TerminosActivacion(cerrar: () -> Unit) {
    AlertDialog(onDismissRequest = cerrar, title = { Text("Términos y privacidad") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("Texto provisional · pendiente de aprobación legal", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("El acceso es personal y permite consultar los datos de tu membresía, pagos, productos y avisos. " +
                "Los pagos y renovaciones se realizan en recepción. Protege tus credenciales y no las compartas. " +
                "Los datos de tu cuenta se utilizan para prestar el servicio y enviarte avisos por la app y WhatsApp. " +
                "Puedes solicitar información o cambios de tus datos y consentimientos en recepción.")
        } }, confirmButton = { TextButton(cerrar) { Text("Entendido") } })
}
