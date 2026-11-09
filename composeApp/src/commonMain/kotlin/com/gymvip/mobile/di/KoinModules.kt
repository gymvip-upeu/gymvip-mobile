package com.gymvip.mobile.di

import com.gymvip.mobile.data.local.DatabaseDriverFactory
import com.gymvip.mobile.data.local.GymVipDatabase
import com.gymvip.mobile.data.local.SecureTokenStore
import com.gymvip.mobile.data.remote.api.*
import com.gymvip.mobile.data.repository.*
import com.gymvip.mobile.data.sync.SyncQueueManager
import com.gymvip.mobile.domain.repository.*
import com.gymvip.mobile.domain.usecase.*
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun appModules(driverFactory: DatabaseDriverFactory, tokenStore: SecureTokenStore): List<Module> = listOf(
    module {
        single { driverFactory.createDriver() }
        single { GymVipDatabase(get()) }
        single { tokenStore }
        single { GymVipHttpClient.create(tokenStore = get()) }
        single { AuthApi(get()) }
        single { MemberApi(get()) }
        single { CatalogApi(get()) }
        single { FavoriteApi(get()) }
        single { SyncQueueManager(get(), get()) }
    },
    module {
        single<AuthRepository> { AuthRepositoryImpl(get(), get(), get(), get()) }
        single<MemberRepository> { MemberRepositoryImpl(get(), get()) }
        single<CatalogRepository> { CatalogRepositoryImpl(get(), get()) }
        single<FavoriteRepository> { FavoriteRepositoryImpl(get(), get(), get(), get()) }
        single<FaqRepository> { FaqRepositoryImpl(get(), get()) }
        single<InboxRepository> { InboxRepositoryImpl(get(), get()) }
        single<GymServiceRepository> { GymServiceRepositoryImpl(get(), get()) }
        single<LegalConsentRepository> { LegalConsentRepositoryImpl(get(), get()) }
        single<ConfigRepository> { ConfigRepositoryImpl() }
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

