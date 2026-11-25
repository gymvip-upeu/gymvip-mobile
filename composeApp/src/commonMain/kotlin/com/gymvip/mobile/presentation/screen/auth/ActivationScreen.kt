package com.gymvip.mobile.presentation.screen.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymvip.mobile.presentation.component.ContextualHelpCard
import com.gymvip.mobile.presentation.component.GymVipLogo
import com.gymvip.mobile.presentation.theme.DangerRed
import com.gymvip.mobile.presentation.theme.LocalGymVipSystemTheme
import com.gymvip.mobile.presentation.theme.SuccessGreen
import com.gymvip.mobile.presentation.theme.accentForeground
import com.gymvip.mobile.presentation.theme.vipOutlinedTextFieldColors
import com.gymvip.mobile.presentation.viewmodel.AuthViewModel

@Composable
fun ActivationScreen(
    viewModel: AuthViewModel,
    onBackToLogin: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val followsSystem = LocalGymVipSystemTheme.current
    val accent = if (followsSystem) MaterialTheme.colorScheme.primary else Color(0xFFD4AF37)

    var activationToken by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    var localValidationError by remember { mutableStateOf<String?>(null) }
    var failedAttempts by remember { mutableIntStateOf(0) }

    val scrollState = rememberScrollState()

    // Reglas de validación en tiempo real (consistentes con Spring Boot MemberWorkspaceService)
    val hasMinLength = newPassword.length >= 12
    val hasUpper = newPassword.any { it.isUpperCase() }
    val hasLower = newPassword.any { it.isLowerCase() }
    val hasDigit = newPassword.any { it.isDigit() }
    val passwordsMatch = newPassword.isNotEmpty() && newPassword == confirmPassword

    fun validateAndSubmit() {
        localValidationError = null
        val tokenClean = activationToken.trim()
        if (tokenClean.isBlank()) {
            localValidationError = "Ingresa el código del enlace de activación recibido por correo."
            return
        }
        if (tokenClean.length < 5) {
            localValidationError = "El código de activación no tiene un formato válido (mínimo 5 caracteres)."
            return
        }
        if (!hasMinLength || !hasUpper || !hasLower || !hasDigit) {
            localValidationError = "La contraseña debe cumplir con todos los requisitos de seguridad."
            return
        }
        if (!passwordsMatch) {
            localValidationError = "Las contraseñas no coinciden. Verifícalas cuidadosamente."
            return
        }
        if (failedAttempts >= 5) {
            localValidationError = "Demasiados intentos fallidos consecutivos. Por favor espera unos momentos antes de reintentar."
            return
        }

        viewModel.activateAccount(tokenClean, newPassword, confirmPassword)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GymVipLogo(size = 64.dp, useGoldVariant = !followsSystem)

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Activación de Cuenta",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Crea tu contraseña personal definitiva con el código de un solo uso entregado en recepción.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // Ayuda contextual opcional
            ContextualHelpCard(
                helpKey = "activation",
                title = "¿Cómo funciona la activación?",
                description = "El código presencial es de un solo uso y tiene una vigencia limitada. Al activarlo, tu usuario se sincroniza con tu ficha de socio.",
                icon = Icons.Outlined.Info
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo: Código de Activación
            OutlinedTextField(
                value = activationToken,
                onValueChange = {
                    activationToken = it
                    localValidationError = null
                },
                label = { Text("Código del enlace de activación") },
                placeholder = { Text("Ej: ACT-9821-X9") },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Outlined.Key, contentDescription = null, tint = accent)
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Ascii
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = vipOutlinedTextFieldColors()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Campo: Nueva Contraseña con Mostrar/Ocultar
            OutlinedTextField(
                value = newPassword,
                onValueChange = {
                    newPassword = it
                    localValidationError = null
                },
                label = { Text("Nueva contraseña personal") },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = accent)
                },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (showPassword) "Ocultar contraseña" else "Mostrar contraseña",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = vipOutlinedTextFieldColors()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Campo: Confirmar Contraseña con Mostrar/Ocultar
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    localValidationError = null
                },
                label = { Text("Confirmar nueva contraseña") },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = accent)
                },
                trailingIcon = {
                    IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                        Icon(
                            imageVector = if (showConfirmPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (showConfirmPassword) "Ocultar confirmación" else "Mostrar confirmación",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = vipOutlinedTextFieldColors()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Checklist Visual de Requisitos de Contraseña
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(12.dp)
            ) {
                Text(
                    text = "REQUISITOS DE SEGURIDAD",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.accentForeground,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                PasswordRequirementRow("Mínimo 12 caracteres", hasMinLength)
                PasswordRequirementRow("Al menos una letra mayúscula (A-Z)", hasUpper)
                PasswordRequirementRow("Al menos una letra minúscula (a-z)", hasLower)
                PasswordRequirementRow("Al menos un número (0-9)", hasDigit)
                PasswordRequirementRow("Las dos contraseñas coinciden", passwordsMatch)
            }

            // Errores locales o del backend
            val activeError = localValidationError ?: state.errorMessage
            if (activeError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DangerRed.copy(alpha = 0.15f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = activeError,
                        color = DangerRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (state.successMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SuccessGreen.copy(alpha = 0.15f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.successMessage ?: "",
                        color = SuccessGreen,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { validateAndSubmit() },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
                } else {
                    Text(text = "Activar Mi Cuenta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            TextButton(
                onClick = onBackToLogin,
                modifier = Modifier.defaultMinSize(minWidth = 120.dp, minHeight = 44.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.accentForeground,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Volver al inicio de sesión",
                    color = MaterialTheme.colorScheme.accentForeground,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PasswordRequirementRow(text: String, isMet: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = if (isMet) Icons.Outlined.CheckCircle else Icons.Outlined.Info,
            contentDescription = null,
            tint = if (isMet) SuccessGreen else Color(0xFF94A3B8),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (isMet) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
