package com.gymvip.mobile.di

import com.gymvip.mobile.data.local.DatabaseDriverFactory
import com.gymvip.mobile.data.local.GymVipDatabase
import com.gymvip.mobile.data.local.SecureTokenStore
import com.gymvip.mobile.data.remote.api.AuthApi
import com.gymvip.mobile.data.remote.api.CatalogApi
import com.gymvip.mobile.data.remote.api.FavoriteApi
import com.gymvip.mobile.data.remote.api.GymVipHttpClient
import com.gymvip.mobile.data.remote.api.MemberApi
import com.gymvip.mobile.data.repository.AuthRepositoryImpl
import com.gymvip.mobile.data.repository.CatalogRepositoryImpl
import com.gymvip.mobile.data.repository.ConfigRepositoryImpl
import com.gymvip.mobile.data.repository.FaqRepositoryImpl
import com.gymvip.mobile.data.repository.FavoriteRepositoryImpl
import com.gymvip.mobile.data.repository.InboxRepositoryImpl
import com.gymvip.mobile.data.repository.MemberRepositoryImpl
import com.gymvip.mobile.data.sync.SyncQueueManager
import com.gymvip.mobile.domain.repository.AuthRepository
import com.gymvip.mobile.domain.repository.CatalogRepository
import com.gymvip.mobile.domain.repository.ConfigRepository
import com.gymvip.mobile.domain.repository.FaqRepository
import com.gymvip.mobile.domain.repository.FavoriteRepository
import com.gymvip.mobile.domain.repository.InboxRepository
import com.gymvip.mobile.domain.repository.MemberRepository
import com.gymvip.mobile.domain.usecase.ActivateAccountUseCase
import com.gymvip.mobile.domain.usecase.GetCatalogUseCase
import com.gymvip.mobile.domain.usecase.GetFaqsUseCase
import com.gymvip.mobile.domain.usecase.GetInboxUseCase
import com.gymvip.mobile.domain.usecase.GetMembershipSummaryUseCase
import com.gymvip.mobile.domain.usecase.GetPaymentHistoryUseCase
import com.gymvip.mobile.domain.usecase.LoginUseCase
import com.gymvip.mobile.domain.usecase.ManageFavoritesUseCase
import com.gymvip.mobile.domain.usecase.SyncFavoritesUseCase

object AppContainer {

    private var initialized = false

    lateinit var database: GymVipDatabase
        private set

    lateinit var secureTokenStore: SecureTokenStore
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var memberRepository: MemberRepository
        private set

    lateinit var catalogRepository: CatalogRepository
        private set

    lateinit var favoriteRepository: FavoriteRepository
        private set

    lateinit var faqRepository: FaqRepository
        private set

    lateinit var inboxRepository: InboxRepository
        private set

    lateinit var configRepository: ConfigRepository
        private set

    // UseCases
    lateinit var loginUseCase: LoginUseCase
        private set

    lateinit var activateAccountUseCase: ActivateAccountUseCase
        private set

    lateinit var getMembershipSummaryUseCase: GetMembershipSummaryUseCase
        private set

    lateinit var getPaymentHistoryUseCase: GetPaymentHistoryUseCase
        private set

    lateinit var getCatalogUseCase: GetCatalogUseCase
        private set

    lateinit var manageFavoritesUseCase: ManageFavoritesUseCase
        private set

    lateinit var syncFavoritesUseCase: SyncFavoritesUseCase
        private set

    lateinit var getFaqsUseCase: GetFaqsUseCase
        private set

    lateinit var getInboxUseCase: GetInboxUseCase
        private set

    fun initialize(
        driverFactory: DatabaseDriverFactory,
        tokenStore: SecureTokenStore
    ) {
        if (initialized) return

        val driver = driverFactory.createDriver()
        database = GymVipDatabase(driver)
        secureTokenStore = tokenStore

        val httpClient = GymVipHttpClient.create(tokenStore = secureTokenStore)

        val authApi = AuthApi(httpClient)
        val memberApi = MemberApi(httpClient)
        val catalogApi = CatalogApi(httpClient)
        val favoriteApi = FavoriteApi(httpClient)

        val syncQueueManager = SyncQueueManager(database, favoriteApi)

        authRepository = AuthRepositoryImpl(authApi, memberApi, secureTokenStore, database)
        memberRepository = MemberRepositoryImpl(memberApi, database)
        catalogRepository = CatalogRepositoryImpl(catalogApi, database)
        favoriteRepository = FavoriteRepositoryImpl(favoriteApi, catalogRepository, database, syncQueueManager)
        faqRepository = FaqRepositoryImpl(database)
        inboxRepository = InboxRepositoryImpl(database)
        configRepository = ConfigRepositoryImpl()

        loginUseCase = LoginUseCase(authRepository)
        activateAccountUseCase = ActivateAccountUseCase(authRepository)
        getMembershipSummaryUseCase = GetMembershipSummaryUseCase(memberRepository)
        getPaymentHistoryUseCase = GetPaymentHistoryUseCase(memberRepository)
        getCatalogUseCase = GetCatalogUseCase(catalogRepository)
        manageFavoritesUseCase = ManageFavoritesUseCase(favoriteRepository)
        syncFavoritesUseCase = SyncFavoritesUseCase(favoriteRepository)
        getFaqsUseCase = GetFaqsUseCase(faqRepository)
        getInboxUseCase = GetInboxUseCase(inboxRepository)

        initialized = true
    }

    val isInitialized: Boolean get() = initialized
}
