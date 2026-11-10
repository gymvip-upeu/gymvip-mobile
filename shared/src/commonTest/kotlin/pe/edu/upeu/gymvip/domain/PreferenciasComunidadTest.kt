package pe.edu.upeu.gymvip.domain

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import pe.edu.upeu.gymvip.auth.AlmacenSesionFake
import pe.edu.upeu.gymvip.data.local.SecureTokenStore
import pe.edu.upeu.gymvip.data.repository.ConfigRepositoryImpl
import pe.edu.upeu.gymvip.domain.model.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class PreferenciasComunidadTest {
    private class PreferenciasStore(val sesion: AlmacenSesionFake = AlmacenSesionFake()) : SecureTokenStore by sesion {
        val preferencias = mutableMapOf<String, String>()
        var falla = false
        override suspend fun getPreference(key: String) = preferencias[key]
        override suspend fun setPreference(key: String, value: String) {
            if (falla) error("Sin almacenamiento")
            preferencias[key] = value
        }
    }
    @Test fun temaYAvisosSeRestauranDesdeElAlmacenPersistente() = runTest {
        val store = PreferenciasStore()
        val repo = ConfigRepositoryImpl(store, backgroundScope)
        repo.setThemePreference("LIGHT"); repo.setExpiryPopups(false)
        val restaurado = ConfigRepositoryImpl(store, backgroundScope)
        runCurrent()
        assertEquals("LIGHT", restaurado.getThemePreference().first())
        assertFalse(restaurado.getExpiryPopups().first())
        store.clear()
        assertEquals("LIGHT", store.preferencias["theme"])
    }
    @Test fun falloDePersistenciaNoPublicaUnCambioQueNoSeGuardo() = runTest {
        val store = PreferenciasStore()
        val repo = ConfigRepositoryImpl(store, backgroundScope)
        repo.setThemePreference("DARK"); store.falla = true
        assertFails { repo.setThemePreference("LIGHT") }
        assertEquals("DARK", repo.getThemePreference().first())
    }
    @Test fun preferenciasInvalidasVuelvenAValoresSeguros() = runTest {
        val store = PreferenciasStore().apply { preferencias["theme"] = "INVALIDO" }
        val repo = ConfigRepositoryImpl(store, backgroundScope)
        runCurrent()
        assertEquals("DARK", repo.getThemePreference().first())
        assertTrue(repo.getExpiryPopups().first())
    }
    @Test fun contactosSeEnmascaranSinAlterarLosYaOcultos() {
        assertEquals("va••••••@mail.com", correoEnmascarado("valeria@mail.com"))
        assertEquals("va••••@mail.com", correoEnmascarado("va••••@mail.com"))
        assertEquals("+51 9•• ••• 482", celularEnmascarado("+51 912 345 482"))
        assertEquals("8•• ••• 482", celularEnmascarado("812345482"))
        assertEquals("Sin registrar", celularEnmascarado(null))
    }
}
