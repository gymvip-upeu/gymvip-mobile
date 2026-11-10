package pe.edu.upeu.gymvip.presentation.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun AlVolver(habilitado: Boolean, accion: () -> Unit) { BackHandler(habilitado, accion) }
