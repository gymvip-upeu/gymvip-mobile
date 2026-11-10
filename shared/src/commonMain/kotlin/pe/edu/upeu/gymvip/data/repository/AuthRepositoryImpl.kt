package pe.edu.upeu.gymvip.data.repository

import pe.edu.upeu.gymvip.data.local.SesionLocal
import pe.edu.upeu.gymvip.data.mapper.toDomain
import pe.edu.upeu.gymvip.data.remote.api.*
import pe.edu.upeu.gymvip.data.remote.dto.*
import pe.edu.upeu.gymvip.domain.error.AppError
import pe.edu.upeu.gymvip.domain.model.AccountStatus
import pe.edu.upeu.gymvip.domain.model.Rol
import pe.edu.upeu.gymvip.domain.model.UsuarioPersonal
import pe.edu.upeu.gymvip.domain.repository.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

class AuthRepositoryImpl(
    private val authApi: AuthApi,
    private val memberApi: MemberApi,
    private val sesion: SesionLocal
) : AuthRepository {
    private val mutex = Mutex()
    private val store get() = sesion.store
    override fun getAuthState(): Flow<AuthState> = sesion.estado.asStateFlow()

    override suspend fun login(email: String, password: String): Result<Unit> = mutex.withLock {
        sesion.cerrar()
        sesion.estado.value = AuthState.Loading
        try {
            val result = resultadoLlamada { iniciar(email, password) }
            if (result.isFailure) sesion.cerrar(result.exceptionOrNull()?.message)
            result
        } catch (cancelled: CancellationException) {
            sesion.cerrar()
            throw cancelled
        }
    }

    private suspend fun iniciar(email: String, password: String) {
        val dto = authApi.login(LoginRequestDto(email, password)).getOrThrow()
        if (Rol.reconocidos(dto.roles).isEmpty()) {
            authApi.logout(dto.token)
            throw AppError.Forbidden("Tu cuenta no tiene un rol habilitado para esta aplicación.")
        }
        if (dto.token.isBlank() || dto.resolvedAccountId.isBlank()) throw AppError.Server(502, "La sesión recibida no es válida.")
        store.saveTokens(dto.token, null, dto.resolvedAccountId)
        // Un cierre durante esta consulta nunca habilita acceso a la aplicación.
        store.saveSessionMetadata(dto.email, true)
        resolverSesion(dto.email, Rol.reconocidos(dto.roles))
    }

    private suspend fun resolverSesion(email: String, rolesLogin: Set<Rol>? = null) {
        // Restaurar siempre desde el servidor; nunca confiar en una preferencia local de rol.
        val current = if (rolesLogin == null || Rol.ADMIN in rolesLogin || Rol.RECEPCION in rolesLogin)
            authApi.currentUser().getOrThrow() else null
        val roles = rolesLogin ?: Rol.reconocidos(current!!.roles.map { it.authority })
        if (roles.isEmpty()) throw AppError.Forbidden("Tu cuenta no tiene un rol habilitado.")
        if (Rol.ADMIN in roles || Rol.RECEPCION in roles) {
            checkNotNull(current)
            val confirmados = Rol.reconocidos(current.roles.map { it.authority })
            if (Rol.ADMIN !in confirmados && Rol.RECEPCION !in confirmados)
                throw AppError.Forbidden("Los permisos de la cuenta cambiaron. Inicia sesión de nuevo.")
            val id = store.getAccountId() ?: throw AppError.Unauthorized("Tu sesión expiró.")
            store.saveSessionMetadata(current.email, false)
            sesion.estado.value = AuthState.StaffAuthenticated(UsuarioPersonal(id, current.email, confirmados))
            return
        }
        val estado = memberApi.getSessionStatus().getOrThrow()
        store.saveSessionMetadata(email, estado.mustChangePassword)
        if (estado.mustChangePassword) {
            sesion.estado.value = AuthState.RequiresPasswordChange(email)
            return
        }
        val dto = memberApi.getProfile().getOrThrow()
        val profile = dto.toDomain(Clock.System.now().toString())
        if (profile.status != AccountStatus.ACTIVO) throw AppError.Forbidden("Tu cuenta está inactiva. Acércate a recepción.")
        // Las cachés del socio usan client.id, no accounts.id.
        val token = store.getAccessToken() ?: throw AppError.Unauthorized("Tu sesión expiró.")
        store.saveTokens(token, null, profile.accountId)
        val q = sesion.database.gymVipDatabaseQueries
        q.insertOrReplaceProfile(profile.accountId, profile.email, profile.firstName, profile.lastName,
            profile.documentType, profile.documentNumber, profile.phone, profile.status.name, profile.lastSyncedAt)
        sesion.estado.value = AuthState.Authenticated(profile)
    }

    override suspend fun checkExistingSession(): Boolean = mutex.withLock {
        val token = store.getAccessToken()
        val email = store.getSessionEmail()
        if (token.isNullOrBlank() || email.isNullOrBlank()) {
            sesion.cerrar()
            sesion.estado.value = AuthState.Unauthenticated()
            return@withLock false
        }
        val result = resultadoLlamada { resolverSesion(email) }
        if (result.isFailure) {
            val error = result.exceptionOrNull()
            if (error is AppError.Unauthorized || error is AppError.Forbidden) sesion.cerrar("Tu sesión expiró. Inicia sesión de nuevo.")
            else sesion.estado.value = AuthState.Unauthenticated(error?.message)
        }
        result.isSuccess
    }

    override suspend fun activateAccount(currentPassword: String, newPassword: String, acceptsTerms: Boolean): Result<Unit> = mutex.withLock {
        val pending = sesion.estado.value as? AuthState.RequiresPasswordChange
            ?: return@withLock Result.failure(AppError.Unauthorized("Ingresa primero con tus credenciales temporales."))
        if (!acceptsTerms) return@withLock Result.failure(AppError.Validation("Acepta los términos para continuar."))
        val result = resultadoLlamada {
            val consent = ConsentSubmitRequestDto(true, true, true, "borrador-mobile-v1")
            memberApi.submitConsents(consent).getOrThrow()
            // El servicio puede ocultar errores SQL; verificar lectura antes de cambiar la contraseña.
            val saved = memberApi.getConsents().getOrThrow()
            if (!saved.termsAccepted || !saved.privacyAcknowledged || !saved.whatsappConsent || saved.documentVersion != consent.documentVersion)
                throw AppError.Server(503, "No se pudo confirmar tu consentimiento. Intenta de nuevo.")
            val changed = memberApi.changePassword(PasswordChangeRequestDto(currentPassword, newPassword)).getOrThrow()
            if (!changed.changed) throw AppError.Server(502, "No se confirmó el cambio de contraseña.")
            // El servidor revoca todas las sesiones, incluso esta. Obtener un token nuevo.
            sesion.cerrar(publicar = false)
            iniciar(pending.email, newPassword)
            if (sesion.estado.value is AuthState.RequiresPasswordChange)
                throw AppError.Server(502, "El servidor aún solicita el cambio de contraseña.")
        }
        if (result.isFailure && sesion.estado.value !is AuthState.RequiresPasswordChange) {
            sesion.cerrar("Si ya cambiaste tu contraseña, inicia sesión con la nueva.")
        }
        result
    }

    override suspend fun logout(): Result<Unit> = mutex.withLock {
        val token = store.getAccessToken()
        sesion.cerrar()
        if (token != null) authApi.logout(token)
        Result.success(Unit)
    }
    override suspend fun getStoredAccountId(): String? = store.getAccountId()
    override suspend fun getRememberedIdentifier(): String? = store.getRememberedIdentifier()
    override suspend fun saveRememberedIdentifier(identifier: String) = store.saveRememberedIdentifier(identifier)
    override suspend fun clearRememberedIdentifier() = store.clearRememberedIdentifier()
    override suspend fun hasSeenOnboarding(): Boolean = store.hasSeenOnboarding()
    override suspend fun completeOnboarding() = store.completeOnboarding()
}
