package com.gymvip.mobile.presentation.screen.profile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.gymvip.mobile.presentation.screen.settings.SettingsPage
import com.gymvip.mobile.presentation.theme.vipOutlinedTextFieldColors
import com.gymvip.mobile.presentation.viewmodel.ProfileViewModel

@Composable
internal fun EditProfileScreen(viewModel: ProfileViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    SettingsPage(title = "Editar perfil", onBack = onBack) {
        val profile = state.profile
        if (profile == null) {
            Text(
                if (state.isLoading) "Cargando perfil…" else "No pudimos cargar tu perfil.",
                color = MaterialTheme.colorScheme.onBackground
            )
            if (!state.isLoading) Button(onClick = viewModel::refresh) { Text("Reintentar") }
        } else {
            var firstName by rememberSaveable(profile.accountId) { mutableStateOf(profile.firstName) }
            var lastName by rememberSaveable(profile.accountId) { mutableStateOf(profile.lastName) }
            var phone by rememberSaveable(profile.accountId) { mutableStateOf(profile.phone.orEmpty()) }
            Text(
                "Los cambios se muestran en tu perfil durante esta sesión.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("Nombres") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = vipOutlinedTextFieldColors()
            )
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Apellidos") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = vipOutlinedTextFieldColors()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Teléfono de contacto") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                colors = vipOutlinedTextFieldColors()
            )
            Button(
                onClick = {
                    viewModel.editLocally(firstName, lastName, phone)
                    onBack()
                },
                enabled = firstName.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar cambios")
            }
        }
    }
}
