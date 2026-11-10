package pe.edu.upeu.gymvip.di

import pe.edu.upeu.gymvip.data.local.DatabaseDriverFactory
import pe.edu.upeu.gymvip.data.local.GymVipDatabase
import pe.edu.upeu.gymvip.data.local.SecureTokenStore
import pe.edu.upeu.gymvip.data.local.SesionLocal
import pe.edu.upeu.gymvip.data.remote.api.*
import pe.edu.upeu.gymvip.data.repository.*
import pe.edu.upeu.gymvip.data.sync.SyncQueueManager
import pe.edu.upeu.gymvip.domain.repository.*
import pe.edu.upeu.gymvip.domain.usecase.*
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun appModules(driverFactory: DatabaseDriverFactory, tokenStore: SecureTokenStore): List<Module> = listOf(
    module {
        single { driverFactory.createDriver() }
        single { GymVipDatabase(get()) }
        single { tokenStore }
        single { SesionLocal(get(), get()) }
        single { GymVipHttpClient.create(tokenStore = get(), onUnauthorized = { token ->
            get<SesionLocal>().cerrar("Tu sesión expiró. Inicia sesión de nuevo.", token)
        }) }
        single { AuthApi(get()) }
        single { PersonalApi(get()) }
        single { ComunidadApi(get()) }
        single { MemberApi(get()) }
        single { CatalogApi(get()) }
        single { FavoriteApi(get()) }
        single { SyncQueueManager(get(), get()) }
    },
    module {
        single<AuthRepository> { AuthRepositoryImpl(get(), get(), get()) }
        single<GestionPersonalRepository> { GestionPersonalRepositoryRest(get(), get()) }
        single<ComunidadRepository> { ComunidadRepositoryRest(get(), get()) }
        single<MemberRepository> { MemberRepositoryImpl(get(), get(), get()) }
        single<CatalogRepository> { CatalogRepositoryImpl(get(), get()) }
        single<FavoriteRepository> { FavoriteRepositoryImpl(get(), get(), get(), get()) }
        single<FaqRepository> { FaqRepositoryImpl(get(), get()) }
        single<InboxRepository> { InboxRepositoryImpl(get(), get()) }
        single<GymServiceRepository> { GymServiceRepositoryImpl(get(), get()) }
        single<LegalConsentRepository> { LegalConsentRepositoryImpl(get(), get()) }
        single<ConfigRepository> { ConfigRepositoryImpl(get()) }
    },
    module {
        factory { LoginUseCase(get()) }
        factory { ActivateAccountUseCase(get()) }
        factory { GetMembershipSummaryUseCase(get()) }
        factory { GetPaymentHistoryUseCase(get()) }
        factory { GetCatalogUseCase(get()) }
        factory { ManageFavoritesUseCase(get()) }
        factory { SyncFavoritesUseCase(get()) }
        factory { GetFaqsUseCase(get()) }
        factory { GetInboxUseCase(get()) }
    }
)

fun initKoin(
    driverFactory: DatabaseDriverFactory,
    tokenStore: SecureTokenStore,
    appDeclaration: KoinAppDeclaration = {}
) = startKoin {
    appDeclaration()
    modules(appModules(driverFactory, tokenStore))
}

