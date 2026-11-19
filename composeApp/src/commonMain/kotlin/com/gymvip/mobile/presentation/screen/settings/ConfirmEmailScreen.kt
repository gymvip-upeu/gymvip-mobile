package com.gymvip.mobile.presentation.screen.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.presentation.theme.vipOutlinedTextFieldColors
import com.gymvip.mobile.presentation.viewmodel.AuthViewModel

@Composable
internal fun ConfirmEmailScreen(viewModel: AuthViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val focus = LocalFocusManager.current
    var email by rememberSaveable { mutableStateOf("") }
    var emailError by rememberSaveable { mutableStateOf(false) }
    var submittedEmail by rememberSaveable { mutableStateOf<String?>(null) }
    val validEmail = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email.trim())

    LaunchedEffect(viewModel) { viewModel.clearMessages() }

    fun submit() {
        if (state.isLoading || submittedEmail == email.trim()) return
        if (!validEmail) {
            emailError = true
            return
        }
        focus.clearFocus()
        val requestedEmail = email.trim()
        viewModel.requestPasswordReset(requestedEmail) { result ->
            if (result.isSuccess) submittedEmail = requestedEmail
        }
    }

    SettingsPage(title = "Confirma tu correo electrónico", onBack = onBack) {
        Text(
            "Ingresa el correo registrado en tu cuenta para solicitar el restablecimiento de tu contraseña.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                emailError = false
                viewModel.clearMessages()
            },
            label = { Text("Correo electrónico") },
            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
            singleLine = true,
            enabled = !state.isLoading,
            isError = emailError,
            supportingText = if (emailError) { { Text("Ingresa un correo electrónico válido.") } } else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            colors = vipOutlinedTextFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        state.successMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium)
        }
        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        Button(
            onClick = { submit() },
            enabled = !state.isLoading && email.isNotBlank() && submittedEmail != email.trim(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).imePadding()
        ) {
            Text(if (state.isLoading) "Solicitando…" else "Restablecer contraseña")
        }
    }
}
