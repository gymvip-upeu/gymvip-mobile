package pe.edu.upeu.gymvip.presentation.viewmodel

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import pe.edu.upeu.gymvip.domain.repository.ConfigRepository

data class PreferencesUiState(val currentTheme: String = "DARK", val expiryPopups: Boolean = true, val error: String? = null)
class PreferencesViewModel(private val configRepository: ConfigRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)) {
    private val _uiState = MutableStateFlow(PreferencesUiState())
    val uiState = _uiState.asStateFlow()
    init {
        configRepository.getThemePreference().onEach { value -> _uiState.update { it.copy(currentTheme = value) } }.launchIn(scope)
        configRepository.getExpiryPopups().onEach { value -> _uiState.update { it.copy(expiryPopups = value) } }.launchIn(scope)
    }
    fun setTheme(theme: String) = guardar { configRepository.setThemePreference(theme) }
    fun setExpiryPopups(enabled: Boolean) = guardar { configRepository.setExpiryPopups(enabled) }
    private fun guardar(block: suspend () -> Unit) { scope.launch {
        try { block(); _uiState.update { it.copy(error = null) } }
        catch (c: CancellationException) { throw c }
        catch (_: Exception) { _uiState.update { it.copy(error = "No se pudo guardar la preferencia. Intenta de nuevo.") } }
    } }
    fun onCleared() { scope.cancel() }
}
