package pe.edu.upeu.gymvip.presentation.screen.membership

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.edu.upeu.gymvip.presentation.component.*
import pe.edu.upeu.gymvip.presentation.viewmodel.SocioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(viewModel: SocioViewModel, volver: () -> Unit) {
    val state by viewModel.state.collectAsState()
    PullToRefreshBox(isRefreshing = state.cargando, onRefresh = viewModel::refresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                TextButton(volver) { Text("‹  Volver") }
                EncabezadoPantalla("Historial de pagos", "Importes y medios registrados en recepción.")
            }
            if (state.error != null) item { ErrorSocio(state, viewModel::refresh) }
            if (state.datos != null) item { BannerSocio(state, viewModel::refresh) }
            if (state.datos == null && state.cargando) item { Esqueleto() }
            state.datos?.let { data ->
                if (data.pagos.isEmpty()) item { EstadoVacio("Aún no tienes pagos registrados.") }
                items(data.pagos, key = { it.id }) { PaymentItemCard(it) }
                item { Text("Los pagos se registran presencialmente en recepción.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}
