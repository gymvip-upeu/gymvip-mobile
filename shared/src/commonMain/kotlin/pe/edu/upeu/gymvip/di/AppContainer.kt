package pe.edu.upeu.gymvip.di

import pe.edu.upeu.gymvip.data.local.DatabaseDriverFactory
import pe.edu.upeu.gymvip.data.local.GymVipDatabase
import pe.edu.upeu.gymvip.data.local.SecureTokenStore
import pe.edu.upeu.gymvip.domain.repository.*
import pe.edu.upeu.gymvip.domain.usecase.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.GlobalContext

object AppContainer : KoinComponent {

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

    lateinit var gymServiceRepository: GymServiceRepository
        private set

    lateinit var legalConsentRepository: LegalConsentRepository
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

        if (GlobalContext.getOrNull() == null) {
            initKoin(driverFactory, tokenStore)
        }

        database = get<GymVipDatabase>()
        secureTokenStore = get<SecureTokenStore>()

        authRepository = get<AuthRepository>()
        memberRepository = get<MemberRepository>()
        catalogRepository = get<CatalogRepository>()
        favoriteRepository = get<FavoriteRepository>()
        faqRepository = get<FaqRepository>()
        inboxRepository = get<InboxRepository>()
        gymServiceRepository = get<GymServiceRepository>()
        legalConsentRepository = get<LegalConsentRepository>()
        configRepository = get<ConfigRepository>()

        loginUseCase = get<LoginUseCase>()
        activateAccountUseCase = get<ActivateAccountUseCase>()
        getMembershipSummaryUseCase = get<GetMembershipSummaryUseCase>()
        getPaymentHistoryUseCase = get<GetPaymentHistoryUseCase>()
        getCatalogUseCase = get<GetCatalogUseCase>()
        manageFavoritesUseCase = get<ManageFavoritesUseCase>()
        syncFavoritesUseCase = get<SyncFavoritesUseCase>()
        getFaqsUseCase = get<GetFaqsUseCase>()
        getInboxUseCase = get<GetInboxUseCase>()

        initialized = true
    }

    val isInitialized: Boolean get() = initialized
    val gestionPersonalRepository: GestionPersonalRepository get() = get()
    val comunidadRepository: ComunidadRepository get() = get()
}
