package com.gymvip.mobile.presentation.screen.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.data.remote.api.MemberSignupApi
import com.gymvip.mobile.data.remote.api.MemberSignupChallenge
import com.gymvip.mobile.data.remote.api.MemberSignupRequest
import com.gymvip.mobile.domain.error.AppError
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

private enum class SignupAvailability { Loading, Open, Closed, Unknown }

@Composable
fun MemberSignupScreen(api: MemberSignupApi, onBack: () -> Unit, onExplore: () -> Unit) {
    val scope = rememberCoroutineScope()
    val links = LocalUriHandler.current
    var available by remember { mutableStateOf(SignupAvailability.Loading) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var dni by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var accepted by remember { mutableStateOf(false) }
    var emailCode by remember { mutableStateOf("") }
    var whatsappCode by remember { mutableStateOf("") }
    var challenge by remember { mutableStateOf<MemberSignupChallenge?>(null) }
    var busy by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(api, attempt) {
        available = SignupAvailability.Loading
        api.isAvailable()
            .onSuccess { available = if (it) SignupAvailability.Open else SignupAvailability.Closed }
            .onFailure { available = SignupAvailability.Unknown }
    }

    fun requestCodes() {
        val request = MemberSignupRequest(firstName.trim(), lastName.trim(), dni, birthDate.trim(),
            email.trim(), "+51" + phone, accepted)
        if (runCatching { LocalDate.parse(request.birthDate) }.isFailure ||
            !request.email.matches(Regex("[^@ ]+@[^@ ]+[.][^@ ]+")) ||
            phone.length != 9 || !phone.startsWith('9')) {
            error = "Revisa la fecha en formato AAAA-MM-DD, el correo y tu número de WhatsApp."
            return
        }
        busy = true
        error = null
        scope.launch {
            api.start(request).onSuccess {
                challenge = it
                emailCode = ""
                whatsappCode = ""
            }.onFailure { error = signupMessage(it) }
            busy = false
        }
    }

    Surface(color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onBack) { Text("Volver a iniciar sesión") }
            Text("Crea tu cuenta", style = MaterialTheme.typography.headlineSmall)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            when {
                available == SignupAvailability.Loading -> CircularProgressIndicator()
                available == SignupAvailability.Unknown -> {
                    Text("No pudimos consultar el registro. Inténtalo de nuevo en unos momentos.")
                    Button(onClick = { attempt++ }) { Text("Reintentar") }
                    TextButton(onClick = onExplore) { Text("Explorar gimnasios") }
                }
                available == SignupAvailability.Closed -> {
                    Text("El registro de cuentas está deshabilitado por el momento.")
                    TextButton(onClick = onExplore) { Text("Explorar gimnasios") }
                }
                completed -> {
                    Text("Revisa tu correo para recibir los pasos de acceso. Si ya tienes una cuenta, puedes entrar con tus datos habituales.")
                    Button(onClick = onBack) { Text("Ir a iniciar sesión") }
                }
                challenge != null -> {
                    val current = challenge ?: return@Column
                    Text("Ingresa los códigos que recibiste por correo y WhatsApp. Vencen en " +
                        current.expiresInMinutes + " minutos.")
                    SignupField("Código de correo", emailCode, { emailCode = it.filter(Char::isDigit).take(6) }, KeyboardType.Number)
                    SignupField("Código de WhatsApp", whatsappCode, { whatsappCode = it.filter(Char::isDigit).take(6) }, KeyboardType.Number)
                    Button(enabled = !busy && emailCode.length == 6 && whatsappCode.length == 6, onClick = {
                        busy = true
                        error = null
                        scope.launch {
                            api.verify(current.challengeId, emailCode, whatsappCode)
                                .onSuccess { completed = true; emailCode = ""; whatsappCode = "" }
                                .onFailure { error = signupMessage(it) }
                            busy = false
                        }
                    }) { Text(if (busy) "Verificando…" else "Confirmar códigos") }
                    TextButton(enabled = !busy, onClick = ::requestCodes) { Text("Solicitar nuevos códigos") }
                    TextButton(enabled = !busy, onClick = { challenge = null; error = null }) { Text("Corregir datos") }
                }
                else -> {
                    Text("Verifica tu correo y WhatsApp para explorar los gimnasios con tu cuenta.")
                    SignupField("Nombres", firstName, { firstName = it.take(100) })
                    SignupField("Apellidos", lastName, { lastName = it.take(100) })
                    SignupField("DNI", dni, { dni = it.filter(Char::isDigit).take(8) }, KeyboardType.Number)
                    SignupField("Fecha de nacimiento (AAAA-MM-DD)", birthDate, { birthDate = it.take(10) })
                    SignupField("Correo electrónico", email, { email = it.take(150) }, KeyboardType.Email)
                    SignupField("WhatsApp (+51)", phone, { phone = it.filter(Char::isDigit).take(9) }, KeyboardType.Phone)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = accepted, onCheckedChange = { accepted = it })
                        Text("Acepto los términos y la política de privacidad.")
                    }
                    Row {
                        TextButton(onClick = { links.openUri("https://gymvip.vercel.app/terminos/") }) { Text("Términos") }
                        TextButton(onClick = { links.openUri("https://gymvip.vercel.app/privacidad/") }) { Text("Privacidad") }
                    }
                    Button(enabled = !busy && accepted && dni.length == 8 && firstName.isNotBlank() && lastName.isNotBlank(),
                        onClick = ::requestCodes) { Text(if (busy) "Enviando…" else "Enviar códigos") }
                }
            }
        }
    }
}

@Composable
private fun SignupField(label: String, value: String, onChange: (String) -> Unit, type: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = type), modifier = Modifier.fillMaxWidth())
}

private fun signupMessage(error: Throwable): String = when (error) {
    is AppError.Validation -> error.message ?: "Revisa los datos del registro."
    else -> "No pudimos completar el registro. Revisa tus datos o vuelve a intentarlo en unos minutos."
}
