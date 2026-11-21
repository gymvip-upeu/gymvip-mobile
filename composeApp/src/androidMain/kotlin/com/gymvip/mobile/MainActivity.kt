package com.gymvip.mobile

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import com.gymvip.mobile.di.AppContainer
import com.gymvip.mobile.presentation.security.LocalBiometricGate
import com.gymvip.mobile.presentation.theme.GoldPrimary
import gymvip_mobile.composeapp.generated.resources.Res
import gymvip_mobile.composeapp.generated.resources.onboarding_welcome
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stoppedWithLock = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_GymVip)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        LocalBiometricGate.isAvailable = {
            BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS
        }
        LocalBiometricGate.authenticate = ::requestBiometric
        LocalBiometricGate.requireLogin = {
            scope.launch {
                AppContainer.authRepository.logout()
                LocalBiometricGate.unlock()
            }
        }
        LocalBiometricGate.enabled = true
        LocalBiometricGate.lock()
        setContent {
            val locked by LocalBiometricGate.locked.collectAsState()
            Box(Modifier.fillMaxSize()) {
                App { dark ->
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    controller.isAppearanceLightStatusBars = !dark
                    controller.isAppearanceLightNavigationBars = !dark
                }
                if (locked) {
                    Image(
                        painter = painterResource(Res.drawable.onboarding_welcome),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                        listOf(Color(0xE60B1320), Color(0xCC142033), Color(0xF20B1320))
                    )))
                    Column(
                        Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("GYM VIP bloqueado", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                        Button(onClick = { requestBiometric { if (it) LocalBiometricGate.unlock() } },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color(0xFF0B1320))) {
                            Text("Desbloquear con biometría")
                        }
                        Button(onClick = LocalBiometricGate.requireLogin,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF203650), contentColor = Color.White)) {
                            Text("Iniciar sesión de nuevo")
                        }
                    }
                }
            }
        }
        scope.launch {
            AppContainer.configRepository.setActiveAccount(AppContainer.secureTokenStore.getAccountId())
            val enabled = AppContainer.configRepository.getBooleanPreference("biometric_lock_enabled", false)
            val hasSession = !AppContainer.secureTokenStore.getAccessToken().isNullOrBlank()
            LocalBiometricGate.enabled = enabled
            if (enabled && hasSession) requestBiometric { if (it) LocalBiometricGate.unlock() }
            else LocalBiometricGate.unlock()
        }
    }

    override fun onStop() {
        if (LocalBiometricGate.enabled) {
            LocalBiometricGate.lock()
            stoppedWithLock = true
        }
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        if (stoppedWithLock) {
            stoppedWithLock = false
            scope.launch {
                if (AppContainer.secureTokenStore.getAccessToken().isNullOrBlank()) LocalBiometricGate.unlock()
                else window.decorView.post {
                    requestBiometric { if (it) LocalBiometricGate.unlock() }
                }
            }
        }
    }

    private fun requestBiometric(result: (Boolean) -> Unit) {
        if (!LocalBiometricGate.isAvailable()) {
            result(false)
            return
        }
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(value: BiometricPrompt.AuthenticationResult) = result(true)
                override fun onAuthenticationError(code: Int, message: CharSequence) = result(false)
            })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloquear GYM VIP")
            .setSubtitle("Confirma tu identidad en este dispositivo")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText("Cancelar")
            .build())
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
