package pe.edu.upeu.gymvip.auth

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import pe.edu.upeu.gymvip.domain.repository.*
import pe.edu.upeu.gymvip.domain.usecase.*
import pe.edu.upeu.gymvip.presentation.viewmodel.AuthViewModel
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private class Fake : AuthRepository {
        val estado = MutableStateFlow<AuthState>(AuthState.Idle)
        var espera: CompletableDeferred<Unit>? = null
        var loginCount = 0
        var actualCorrecta = false
        var onboarding = false
        override fun getAuthState(): Flow<AuthState> = estado
        override suspend fun login(email: String, password: String): Result<Unit> {
            loginCount++
            estado.value = AuthState.RequiresPasswordChange(email)
            return Result.success(Unit)
        }
        override suspend fun logout(): Result<Unit> { estado.value = AuthState.Unauthenticated(); return Result.success(Unit) }
        override suspend fun activateAccount(currentPassword: String, newPassword: String, acceptsTerms: Boolean): Result<Unit> {
            actualCorrecta = currentPassword == "Temporal12345"
            return Result.success(Unit)
        }
        override suspend fun getStoredAccountId(): String? = null
        override suspend fun checkExistingSession(): Boolean { espera?.await(); estado.value = AuthState.Unauthenticated(); return false }
        override suspend fun getRememberedIdentifier(): String? = null
        override suspend fun saveRememberedIdentifier(identifier: String) = Unit
        override suspend fun clearRememberedIdentifier() = Unit
        override suspend fun hasSeenOnboarding() = onboarding
        override suspend fun completeOnboarding() { onboarding = true }
    }

    @Test fun esperaValidacionAntesDeResolverNavegacion() = runTest {
        val fake = Fake().apply { espera = CompletableDeferred() }
        val vm = AuthViewModel(LoginUseCase(fake), ActivateAccountUseCase(fake), fake, backgroundScope)
        runCurrent()
        assertFalse(vm.uiState.value.sessionChecked)
        fake.espera!!.complete(Unit); runCurrent()
        assertTrue(vm.uiState.value.sessionChecked)
        assertFalse(vm.uiState.value.isAuthenticated)
        vm.onCleared()
    }

    @Test fun primerIngresoMantienePasswordSoloEnMemoriaYNoAutentica() = runTest {
        val fake = Fake()
        val vm = AuthViewModel(LoginUseCase(fake), ActivateAccountUseCase(fake), fake, backgroundScope)
        runCurrent()
        vm.login("socio@example.test", "Temporal12345")
        vm.login("socio@example.test", "Temporal12345")
        runCurrent()
        assertEquals(1, fake.loginCount)
        assertTrue(vm.uiState.value.mustChangePassword)
        assertFalse(vm.uiState.value.isAuthenticated)
        assertFalse(vm.uiState.value.needsTemporaryPassword)
        vm.activateAccount("", "MiNuevaClave123", "MiNuevaClave123", true)
        runCurrent()
        assertTrue(fake.actualCorrecta)
        vm.onCleared()
    }

    @Test fun onboardingSeCompletaSoloAlElegirAcceso() = runTest {
        val fake = Fake()
        val vm = AuthViewModel(LoginUseCase(fake), ActivateAccountUseCase(fake), fake, backgroundScope)
        runCurrent()
        assertFalse(vm.uiState.value.onboardingSeen)
        var navegacion = false
        vm.completeOnboarding { navegacion = true }; runCurrent()
        assertTrue(fake.onboarding); assertTrue(navegacion)
        vm.onCleared()
    }
}
