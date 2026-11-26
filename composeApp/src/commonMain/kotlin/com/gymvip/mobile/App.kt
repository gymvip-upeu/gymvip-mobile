package com.gymvip.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.gymvip.mobile.di.AppContainer
import com.gymvip.mobile.notification.syncPushRegistration
import com.gymvip.mobile.features.gyms.GymsExplorerScreen
import com.gymvip.mobile.features.reservations.ClassesReservationsScreen
import com.gymvip.mobile.features.training.ExerciseCatalogScreen
import com.gymvip.mobile.features.training.TrainingHubScreen
import com.gymvip.mobile.features.training.TrainingProgressScreen
import com.gymvip.mobile.presentation.component.VipDrawerContent
import com.gymvip.mobile.presentation.component.VipNavigationBar
import com.gymvip.mobile.presentation.component.VipTopAppBar
import com.gymvip.mobile.presentation.navigation.AppBackHandler
import com.gymvip.mobile.presentation.navigation.MainTab
import com.gymvip.mobile.presentation.navigation.NavDestination
import com.gymvip.mobile.presentation.screen.auth.ActivationScreen
import com.gymvip.mobile.presentation.screen.auth.LoginScreen
import com.gymvip.mobile.presentation.screen.auth.PasswordRecoveryScreen
import com.gymvip.mobile.presentation.screen.catalog.AdminProductScannerScreen
import com.gymvip.mobile.presentation.screen.catalog.CatalogScreen
import com.gymvip.mobile.presentation.screen.catalog.CartScreen
import com.gymvip.mobile.domain.model.ShoppingCart
import com.gymvip.mobile.domain.model.SupplementOrder
import com.gymvip.mobile.domain.model.DigitalPaymentLink
import com.gymvip.mobile.domain.model.OrderStatus
import kotlin.random.Random
import com.gymvip.mobile.presentation.screen.faq.FaqScreen
import com.gymvip.mobile.presentation.screen.home.HomeScreen
import com.gymvip.mobile.presentation.screen.inbox.InboxScreen
import com.gymvip.mobile.presentation.screen.legal.TermsAndPrivacyScreen
import com.gymvip.mobile.presentation.screen.membership.MemberCheckInScannerScreen
import com.gymvip.mobile.presentation.screen.membership.MembershipScreen
import com.gymvip.mobile.presentation.screen.permissions.AppPermissionsScreen
import com.gymvip.mobile.presentation.screen.permissions.PostLoginPermissionsScreen
import com.gymvip.mobile.presentation.screen.preferences.PreferencesScreen
import com.gymvip.mobile.presentation.screen.profile.EditProfileScreen
import com.gymvip.mobile.presentation.screen.profile.ProfileScreen
import com.gymvip.mobile.presentation.screen.settings.AccountSecurityScreen
import com.gymvip.mobile.presentation.screen.settings.ConfirmEmailScreen
import com.gymvip.mobile.presentation.screen.settings.SettingsScreen
import com.gymvip.mobile.presentation.screen.services.ServicesScreen
import com.gymvip.mobile.presentation.screen.splash.OnboardingScreen
import com.gymvip.mobile.presentation.screen.splash.SplashScreen
import com.gymvip.mobile.presentation.security.LocalBiometricGate
import com.gymvip.mobile.presentation.theme.GymVipTheme
import com.gymvip.mobile.presentation.viewmodel.AuthViewModel
import com.gymvip.mobile.presentation.viewmodel.CatalogViewModel
import com.gymvip.mobile.presentation.viewmodel.FavoritesViewModel
import com.gymvip.mobile.presentation.viewmodel.HomeViewModel
import com.gymvip.mobile.presentation.viewmodel.InboxViewModel
import com.gymvip.mobile.presentation.viewmodel.MembershipViewModel
import com.gymvip.mobile.presentation.viewmodel.PreferencesViewModel
import com.gymvip.mobile.presentation.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

@Composable
fun App(onDarkThemeChanged: ((Boolean) -> Unit)? = null) {
    // Preferencias de tema
    val preferencesViewModel = remember {
        PreferencesViewModel(AppContainer.configRepository)
    }
    val preferencesState by preferencesViewModel.uiState.collectAsState()

    GymVipTheme(
        themePreference = preferencesState.currentTheme,
        fontScale = preferencesState.fontScale,
        highContrast = preferencesState.highContrast,
        reduceMotion = preferencesState.reduceMotion,
        showGuides = preferencesState.showGuides,
        onDarkThemeChanged = onDarkThemeChanged
    ) {
        val authViewModel = remember {
            AuthViewModel(
                loginUseCase = AppContainer.loginUseCase,
                activateAccountUseCase = AppContainer.activateAccountUseCase,
                authRepository = AppContainer.authRepository
            )
        }
        val authState by authViewModel.uiState.collectAsState()
        LaunchedEffect(authState.isAuthenticated) {
            while (authState.isAuthenticated) {
                AppContainer.authRepository.validateActiveSession()
                kotlinx.coroutines.delay(15_000)
            }
        }

        LaunchedEffect(authState.isAuthenticated, authState.authenticatedProfile?.accountId) {
            val accountId = authState.authenticatedProfile?.accountId?.takeIf { authState.isAuthenticated }
            AppContainer.configRepository.setActiveAccount(accountId)
            LocalBiometricGate.enabled = if (accountId != null) {
                AppContainer.configRepository.getBooleanPreference("biometric_lock_enabled", false)
            } else false
            if (accountId == null) LocalBiometricGate.unlock()
        }

        LaunchedEffect(authState.isAuthenticated, authState.authenticatedProfile?.accountId) {
            if (authState.isAuthenticated && authState.authenticatedProfile?.roles?.contains("CLIENTE") == true) {
                syncPushRegistration()
            }
        }

        var currentDestination by remember { mutableStateOf<NavDestination>(NavDestination.Splash) }
        var currentTab by remember { mutableStateOf(MainTab.HOME) }

        val profileAccountId = authState.authenticatedProfile?.accountId?.takeIf { it.isNotBlank() }
        val pendingPush by com.gymvip.mobile.notification.PushNavigation.pending.collectAsState()
        val inboxRevision by com.gymvip.mobile.notification.PushNavigation.inboxRevision.collectAsState()
        var requestedInboxItem by remember(profileAccountId) { mutableStateOf<String?>(null) }
        var shoppingCart by remember(profileAccountId) { mutableStateOf(ShoppingCart()) }
        var orders by remember(profileAccountId) { mutableStateOf<List<SupplementOrder>>(emptyList()) }
        var paymentLink by remember(profileAccountId) { mutableStateOf<DigitalPaymentLink?>(null) }
        var culqiEnabled by remember(profileAccountId) { mutableStateOf(false) }
        var cartError by remember(profileAccountId) { mutableStateOf<String?>(null) }
        var refreshingOrders by remember(profileAccountId) { mutableStateOf(false) }
        var checkoutKey by remember(profileAccountId) { mutableStateOf<String?>(null) }
        val orderApi = AppContainer.supplementOrderApi
        suspend fun refreshOrders() {
            if (refreshingOrders) return
            refreshingOrders = true
            cartError = null
            try {
                orderApi.cart().onSuccess { shoppingCart = it }.onFailure { cartError = it.message }
                orderApi.orders().onSuccess { remote ->
                    orders = remote.map { order ->
                        if (order.hasPickupToken && order.orderStatus == OrderStatus.LISTO_PARA_RETIRO) {
                            val token = orderApi.pickupToken(order.id)
                                .onFailure { cartError = it.message }.getOrNull()
                            order.copy(pickupToken = token, hasPickupToken = token != null)
                        } else order
                    }
                    val digital = orders.firstOrNull { it.paymentMethod == "TARJETA_PASARELA" && it.paymentStatus == com.gymvip.mobile.domain.model.PaymentStatus.PENDIENTE }
                    if (digital != null) {
                        orderApi.paymentLinkForOrder(digital.id).onSuccess { paymentLink = it }
                            .onFailure { cartError = it.message }
                    } else paymentLink = null
                }.onFailure { cartError = it.message }
            } finally {
                refreshingOrders = false
            }
        }
        LaunchedEffect(profileAccountId, authState.isAuthenticated) {
            if (authState.isAuthenticated && authState.authenticatedProfile?.roles?.contains("CLIENTE") == true) {
                orderApi.paymentConfig().onSuccess { culqiEnabled = it }
                refreshOrders()
            }
        }
        val profileViewModel = if (authState.isAuthenticated && profileAccountId != null) {
            remember(profileAccountId) {
                ProfileViewModel(memberRepository = AppContainer.memberRepository, accountId = profileAccountId)
            }
        } else null
        DisposableEffect(profileViewModel) {
            onDispose { profileViewModel?.onCleared() }
        }

        val parentDestination = when (val destination = currentDestination) {
            NavDestination.Settings -> NavDestination.Main(MainTab.PROFILE)
            NavDestination.Faq, NavDestination.Services, NavDestination.MemberCheckInScanner,
            NavDestination.GymExplorer, NavDestination.ClassReservations -> NavDestination.Main(MainTab.HOME)
            NavDestination.ExerciseCatalog, NavDestination.TrainingProgress -> NavDestination.Main(MainTab.TRAINING)
            NavDestination.EditProfile, NavDestination.Preferences, NavDestination.Permissions,
            NavDestination.AccountSecurity -> NavDestination.Settings
            NavDestination.ConfirmEmail -> NavDestination.AccountSecurity
            NavDestination.Cart -> NavDestination.Main(MainTab.CATALOG)
            is NavDestination.TermsAndPrivacy -> if (destination.requiresAcceptance) null else NavDestination.Settings
            else -> null
        }
        AppBackHandler(enabled = parentDestination != null) {
            parentDestination?.let { currentDestination = it }
        }

        val coroutineScope = rememberCoroutineScope()

        // Una sesión autenticada no entra al contenido hasta aceptar términos y privacidad.
        LaunchedEffect(authState.isAuthenticated, authState.authenticatedProfile?.accountId, authState.mustChangePassword) {
            if (authState.mustChangePassword) {
                currentDestination = NavDestination.FirstPassword
                return@LaunchedEffect
            }
            if (authState.isAuthenticated) {
                val accountId = authState.authenticatedProfile?.accountId
                if (accountId.isNullOrBlank()) {
                    currentDestination = NavDestination.Login
                    return@LaunchedEffect
                }

                AppContainer.configRepository.setActiveAccount(accountId)
                currentTab = MainTab.HOME

                val consent = AppContainer.legalConsentRepository.fetchConsent(accountId).getOrNull()
                val hasRequiredConsent = consent?.let {
                    it.termsAccepted && it.privacyAcknowledged && it.documentVersion == "v1.0"
                } ?: false

                if (!hasRequiredConsent) {
                    currentDestination = NavDestination.TermsAndPrivacy(requiresAcceptance = true)
                } else {
                    val permissionsSeen = AppContainer.configRepository.getBooleanPreference("permissions_explainer_completed", false)
                    if (!permissionsSeen) {
                        currentDestination = NavDestination.PostLoginPermissions
                    } else {
                        when (val destination = currentDestination) {
                            is NavDestination.Splash,
                            is NavDestination.Onboarding,
                            is NavDestination.Login,
                            is NavDestination.Activation,
                            is NavDestination.PasswordRecovery,
                            is NavDestination.PostLoginPermissions -> currentDestination = NavDestination.Main(currentTab)
                            is NavDestination.TermsAndPrivacy -> {
                                if (destination.requiresAcceptance) currentDestination = NavDestination.Main(currentTab)
                            }
                            else -> Unit
                        }
                    }
                }
            } else if (
                currentDestination !is NavDestination.Splash &&
                currentDestination !is NavDestination.Onboarding &&
                currentDestination !is NavDestination.Login &&
                currentDestination !is NavDestination.PasswordRecovery &&
                currentDestination !is NavDestination.Activation
            ) {
                currentTab = MainTab.HOME
                val onboardingSeen = AppContainer.configRepository.getBooleanPreference("onboarding_completed", false)
                currentDestination = if (onboardingSeen) NavDestination.Login else NavDestination.Onboarding
            }
        }

        LaunchedEffect(pendingPush, profileAccountId, authState.isAuthenticated, currentDestination) {
            val target = pendingPush ?: return@LaunchedEffect
            if (!authState.isAuthenticated) return@LaunchedEffect
            if (target.accountId != profileAccountId || authState.authenticatedProfile?.roles?.contains("CLIENTE") != true) {
                com.gymvip.mobile.notification.PushNavigation.clear()
                return@LaunchedEffect
            }
            val destination = currentDestination
            if (destination is NavDestination.Splash || destination is NavDestination.Login ||
                destination is NavDestination.PostLoginPermissions ||
                (destination is NavDestination.TermsAndPrivacy && destination.requiresAcceptance)) return@LaunchedEffect
            if (target.destination == com.gymvip.mobile.notification.PushDestination.ORDERS) {
                currentDestination = NavDestination.Cart
                com.gymvip.mobile.notification.PushNavigation.clear()
                refreshOrders()
                return@LaunchedEffect
            }
            requestedInboxItem = target.itemId
            currentTab = MainTab.INBOX
            currentDestination = NavDestination.Main(MainTab.INBOX)
            com.gymvip.mobile.notification.PushNavigation.clear()
        }

        when (val dest = currentDestination) {
            NavDestination.FirstPassword -> com.gymvip.mobile.presentation.screen.auth.FirstPasswordScreen(authViewModel)
            is NavDestination.Splash -> {
                SplashScreen(
                    onResolved = { isActive ->
                        if (!isActive) {
                            coroutineScope.launch {
                                val onboardingSeen = AppContainer.configRepository.getBooleanPreference("onboarding_completed", false)
                                currentDestination = if (onboardingSeen) NavDestination.Login else NavDestination.Onboarding
                            }
                        }
                    },
                    isSessionActive = authState.isAuthenticated
                )
            }

            is NavDestination.Onboarding -> {
                OnboardingScreen(
                    onLogin = {
                        coroutineScope.launch {
                            AppContainer.configRepository.setBooleanPreference("onboarding_completed", true)
                        }
                        currentDestination = NavDestination.Login
                    },
                    onActivate = {
                        coroutineScope.launch {
                            AppContainer.configRepository.setBooleanPreference("onboarding_completed", true)
                        }
                        currentDestination = NavDestination.Activation
                    }
                )
            }

            is NavDestination.PostLoginPermissions -> {
                PostLoginPermissionsScreen(
                    onContinue = {
                        coroutineScope.launch {
                            AppContainer.configRepository.setBooleanPreference("permissions_explainer_completed", true)
                        }
                        currentDestination = NavDestination.Main(currentTab)
                    },
                    onSkip = {
                        coroutineScope.launch {
                            AppContainer.configRepository.setBooleanPreference("permissions_explainer_completed", true)
                        }
                        currentDestination = NavDestination.Main(currentTab)
                    }
                )
            }

            is NavDestination.Login -> {
                LoginScreen(
                    viewModel = authViewModel,
                    onNavigateToActivation = { currentDestination = NavDestination.Activation },
                    onNavigateToRecovery = { currentDestination = NavDestination.PasswordRecovery }
                )
            }

            is NavDestination.PasswordRecovery -> {
                PasswordRecoveryScreen(
                    viewModel = authViewModel,
                    onBackToLogin = { currentDestination = NavDestination.Login }
                )
            }

            is NavDestination.AdminScanner -> {
                val userRoles = authState.authenticatedProfile?.roles.orEmpty()
                AdminProductScannerScreen(
                    catalogRepository = AppContainer.catalogRepository,
                    userRoles = userRoles,
                    onBack = { currentDestination = NavDestination.Main(currentTab) }
                )
            }

            is NavDestination.Cart -> {
                if (authState.isAuthenticated && authState.authenticatedProfile?.roles?.contains("CLIENTE") == true) {
                    CartScreen(
                        cart = shoppingCart,
                        activeOrder = orders.firstOrNull { it.orderStatus !in setOf(OrderStatus.ENTREGADO, OrderStatus.CANCELADO, OrderStatus.RECHAZADO, OrderStatus.EXPIRADO, OrderStatus.DEVUELTO) },
                        orders = orders,
                        digitalPaymentLink = paymentLink,
                        culqiEnabled = culqiEnabled,
                        errorMessage = cartError,
                        onQuantityChange = { productId, quantity ->
                            coroutineScope.launch {
                                orderApi.setQuantity(productId, quantity).onSuccess { shoppingCart = it; cartError = null }
                                    .onFailure { cartError = it.message }
                            }
                        },
                        onClearCart = {
                            coroutineScope.launch {
                                orderApi.clearCart().onSuccess { shoppingCart = ShoppingCart(); cartError = null }
                                    .onFailure { cartError = it.message }
                            }
                        },
                        onCheckoutOrder = { method, notes ->
                            coroutineScope.launch {
                                val key = checkoutKey ?: "mobile-${profileAccountId}-${Random.nextLong().toULong()}".also { checkoutKey = it }
                                if (method == "TARJETA_PASARELA") {
                                    orderApi.checkoutDigital(key, notes).onSuccess {
                                        paymentLink = it
                                        checkoutKey = null
                                        cartError = null
                                        refreshOrders()
                                    }.onFailure { cartError = it.message }
                                } else {
                                    orderApi.checkoutCash(key, notes).onSuccess {
                                        checkoutKey = null
                                        cartError = null
                                        refreshOrders()
                                    }.onFailure { cartError = it.message }
                                }
                            }
                        },
                        onRefreshPaymentLink = { coroutineScope.launch { refreshOrders() } },
                        onCancelOrder = { reason ->
                            val active = orders.firstOrNull { it.orderStatus == OrderStatus.PENDIENTE_PAGO }
                            if (active != null) coroutineScope.launch {
                                orderApi.cancel(active.id, reason).onSuccess { refreshOrders() }
                                    .onFailure { cartError = it.message }
                            }
                        },
                        onRefresh = { coroutineScope.launch { refreshOrders() } },
                        onBack = { currentDestination = NavDestination.Main(MainTab.CATALOG) }
                    )
                }
            }

            is NavDestination.MemberCheckInScanner -> {
                MemberCheckInScannerScreen(
                    memberRepository = AppContainer.memberRepository,
                    onBack = { currentDestination = NavDestination.Main(MainTab.HOME) }
                )
            }

            is NavDestination.GymExplorer -> {
                val accountId = authState.authenticatedProfile?.accountId ?: "public"
                GymsExplorerScreen(
                    accountId = accountId,
                    preferredBranchName = preferencesState.preferredBranchName,
                    repository = AppContainer.enterpriseTrainingRepository,
                    onBranchSelected = { currentDestination = NavDestination.Main(currentTab) },
                    onBack = { currentDestination = NavDestination.Main(currentTab) }
                )
            }

            is NavDestination.Journal -> {
                val profile = authState.authenticatedProfile
                if (authState.isAuthenticated && profile?.roles?.contains("CLIENTE") == true) {
                    com.gymvip.mobile.features.journal.JournalScreen(
                        accountId = profile.accountId,
                        memberName = profile.fullName,
                        repository = AppContainer.journalRepository,
                        onBack = { currentDestination = NavDestination.Main(MainTab.TRAINING) }
                    )
                }
            }

            is NavDestination.ExerciseCatalog -> {
                val accountId = authState.authenticatedProfile?.accountId ?: "public"
                ExerciseCatalogScreen(
                    accountId = accountId,
                    repository = AppContainer.enterpriseTrainingRepository,
                    onBack = { currentDestination = NavDestination.Main(MainTab.TRAINING) }
                )
            }

            is NavDestination.TrainingProgress -> {
                val accountId = authState.authenticatedProfile?.accountId ?: "public"
                TrainingProgressScreen(
                    accountId = accountId,
                    repository = AppContainer.enterpriseTrainingRepository,
                    onBack = { currentDestination = NavDestination.Main(MainTab.TRAINING) }
                )
            }

            is NavDestination.ClassReservations -> {
                ClassesReservationsScreen(
                    repository = AppContainer.enterpriseTrainingRepository,
                    onBack = { currentDestination = NavDestination.Main(MainTab.HOME) }
                )
            }

            is NavDestination.Activation -> {
                ActivationScreen(
                    viewModel = authViewModel,
                    onBackToLogin = { currentDestination = NavDestination.Login }
                )
            }

            is NavDestination.Faq -> {
                FaqScreen(
                    getFaqsUseCase = AppContainer.getFaqsUseCase,
                    receptionWhatsAppNumber = AppContainer.configRepository.receptionWhatsAppNumber,
                    onBack = { currentDestination = NavDestination.Main(MainTab.HOME) }
                )
            }

            is NavDestination.Services -> {
                ServicesScreen(
                    repository = AppContainer.gymServiceRepository,
                    onBack = { currentDestination = NavDestination.Main(MainTab.HOME) }
                )
            }

            is NavDestination.Settings -> {
                SettingsScreen(
                    onBack = { currentDestination = NavDestination.Main(MainTab.PROFILE) },
                    onEditProfile = { currentDestination = NavDestination.EditProfile },
                    canEditProfile = authState.authenticatedProfile?.roles?.contains("CLIENTE") == true,
                    onAccountSecurity = { currentDestination = NavDestination.AccountSecurity },
                    onPermissions = { currentDestination = NavDestination.Permissions },
                    onAppearance = { currentDestination = NavDestination.Preferences },
                    onTermsAndPrivacy = { currentDestination = NavDestination.TermsAndPrivacy() },
                    onLogout = authViewModel::logout
                )
            }

            is NavDestination.AccountSecurity -> {
                profileViewModel?.let {
                    AccountSecurityScreen(
                        viewModel = it,
                        onBack = { currentDestination = NavDestination.Settings },
                        onChangePassword = { currentDestination = NavDestination.ConfirmEmail },
                        isMember = authState.authenticatedProfile?.roles?.contains("CLIENTE") == true,
                        onAccountDeleted = {
                            authViewModel.logout()
                            currentDestination = NavDestination.Login
                        }
                    )
                }
            }

            is NavDestination.ConfirmEmail -> {
                ConfirmEmailScreen(
                    viewModel = authViewModel,
                    registeredEmail = authState.authenticatedProfile?.email.orEmpty(),
                    onBack = { currentDestination = NavDestination.AccountSecurity }
                )
            }

            is NavDestination.EditProfile -> {
                profileViewModel?.let {
                    EditProfileScreen(viewModel = it, onBack = { currentDestination = NavDestination.Settings })
                }
            }

            is NavDestination.TermsAndPrivacy -> {
                val accountId = authState.authenticatedProfile?.accountId ?: "unknown_account"
                TermsAndPrivacyScreen(
                    repository = AppContainer.legalConsentRepository,
                    accountId = accountId,
                    requiredAcceptance = dest.requiresAcceptance,
                    onBackOrAccepted = {
                        currentDestination = if (dest.requiresAcceptance) {
                            NavDestination.Main(currentTab)
                        } else NavDestination.Settings
                    }
                )
            }

            is NavDestination.Preferences -> {
                PreferencesScreen(
                    viewModel = preferencesViewModel,
                    onBack = { currentDestination = NavDestination.Settings }
                )
            }

            is NavDestination.Permissions -> {
                AppPermissionsScreen(onBack = { currentDestination = NavDestination.Settings })
            }

            is NavDestination.Main -> {
                if (!authState.isAuthenticated) {
                    LoginScreen(
                        viewModel = authViewModel,
                        onNavigateToActivation = { currentDestination = NavDestination.Activation },
                        onNavigateToRecovery = { currentDestination = NavDestination.PasswordRecovery }
                    )
                    return@GymVipTheme
                }
                val accountId = authState.authenticatedProfile?.accountId ?: "unknown_account"
                val userRoles = authState.authenticatedProfile?.roles.orEmpty()
                val isClient = userRoles.contains("CLIENTE")
                val isStaff = userRoles.any { it != "CLIENTE" } && !isClient
                val isExecutive = userRoles.any { it == "ADMIN" || it == "SUPER_ADMIN" || it == "ADMIN_EMPRESA" || it == "GERENTE" || it == "GERENTE_GENERAL" || it == "GERENTE_SEDE" }
                val visibleTabs = when {
                    isClient -> listOf(MainTab.HOME, MainTab.MEMBERSHIP, MainTab.TRAINING, MainTab.CATALOG, MainTab.PROFILE)
                    userRoles.contains("AUDITOR") || userRoles.contains("AUDITOR_EMPRESA") -> listOf(MainTab.HOME, MainTab.PROFILE)
                    else -> listOf(MainTab.HOME, MainTab.CATALOG, MainTab.INBOX, MainTab.PROFILE)
                }

                LaunchedEffect(preferencesState.showGuides, authState.isAuthenticated) {
                    if (authState.isAuthenticated) {
                        AppContainer.enterpriseTrainingRepository.syncShowGuidesToServer(preferencesState.showGuides)
                    }
                }

                // ViewModels para la sesión del socio
                val homeViewModel = remember(accountId) {
                    HomeViewModel(
                        memberRepository = AppContainer.memberRepository,
                        getMembershipSummaryUseCase = AppContainer.getMembershipSummaryUseCase,
                        configRepository = AppContainer.configRepository,
                        accountId = accountId,
                        isStaff = isStaff
                    )
                }

                val membershipViewModel = remember(accountId) {
                    MembershipViewModel(
                        getMembershipSummaryUseCase = AppContainer.getMembershipSummaryUseCase,
                        getPaymentHistoryUseCase = AppContainer.getPaymentHistoryUseCase,
                        accountId = accountId,
                        isStaff = isStaff,
                        plansRepository = AppContainer.membershipPlansRepository
                    )
                }

                val catalogViewModel = remember(accountId) {
                    CatalogViewModel(getCatalogUseCase = AppContainer.getCatalogUseCase)
                }

                val favoritesViewModel = remember(accountId) {
                    FavoritesViewModel(
                        manageFavoritesUseCase = AppContainer.manageFavoritesUseCase,
                        syncFavoritesUseCase = AppContainer.syncFavoritesUseCase,
                        accountId = accountId
                    )
                }

                val inboxViewModel = remember(accountId) {
                    InboxViewModel(
                        getInboxUseCase = AppContainer.getInboxUseCase,
                        accountId = accountId
                    )
                }
                DisposableEffect(inboxViewModel) {
                    onDispose { inboxViewModel.onCleared() }
                }
                LaunchedEffect(requestedInboxItem, accountId) {
                    requestedInboxItem?.let { inboxViewModel.openFromPush(it); requestedInboxItem = null }
                }
                LaunchedEffect(inboxRevision) {
                    if (inboxRevision > 0) inboxViewModel.refresh()
                }

                val drawerState = rememberDrawerState(DrawerValue.Closed)
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        VipDrawerContent(
                            memberName = homeViewModel.uiState.collectAsState().value.profile?.firstName
                                ?: authState.authenticatedProfile?.email.orEmpty(),
                            memberEmail = authState.authenticatedProfile?.email.orEmpty(),
                            preferredBranchName = preferencesState.preferredBranchName,
                            isClient = isClient,
                            isStaff = isStaff,
                            currentDestination = currentDestination,
                            currentTab = currentTab,
                            onSelectTab = { tab ->
                                currentTab = tab
                                currentDestination = NavDestination.Main(tab)
                            },
                            onNavigate = { target -> currentDestination = target },
                            onLogout = authViewModel::logout,
                            onCloseDrawer = { coroutineScope.launch { drawerState.close() } }
                        )
                    }
                ) {
                    Scaffold(
                        topBar = {
                            VipTopAppBar(
                                title = when (currentTab) {
                                    MainTab.HOME -> "GYM VIP"
                                    MainTab.MEMBERSHIP -> "Membresía"
                                    MainTab.TRAINING -> "Entrenamiento"
                                    MainTab.CATALOG -> "Tienda y Planes"
                                    MainTab.INBOX -> "Avisos"
                                    MainTab.PROFILE -> "Mi Perfil"
                                },
                                onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                                onOpenInbox = if (currentTab != MainTab.INBOX) {
                                    {
                                        currentTab = MainTab.INBOX
                                        currentDestination = NavDestination.Main(MainTab.INBOX)
                                    }
                                } else null,
                                onOpenSettings = if (currentTab == MainTab.PROFILE) {
                                    { currentDestination = NavDestination.Settings }
                                } else null,
                                onSyncClick = {
                                    catalogViewModel.refresh()
                                    favoritesViewModel.syncPending()
                                    membershipViewModel.refresh()
                                    homeViewModel.refreshAll()
                                    inboxViewModel.refresh()
                                    coroutineScope.launch {
                                        AppContainer.enterpriseTrainingRepository.syncPendingWorkouts(accountId)
                                        if (userRoles.contains("CLIENTE")) refreshOrders()
                                    }
                                }
                            )
                        },
                        bottomBar = {
                            VipNavigationBar(
                                currentTab = currentTab,
                                visibleTabs = visibleTabs,
                                onTabSelected = { tab ->
                                    if (tab in visibleTabs) {
                                        currentTab = tab
                                        currentDestination = NavDestination.Main(tab)
                                        com.gymvip.mobile.telemetry.VipAnalyticsTracker.logScreenView(tab.name)
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentTab) {
                                MainTab.HOME -> HomeScreen(
                                    viewModel = homeViewModel,
                                    isStaff = isStaff,
                                    userRoles = userRoles,
                                    preferredBranchName = preferencesState.preferredBranchName,
                                    onNavigateServices = { currentDestination = NavDestination.Services },
                                    onNavigateFaq = { currentDestination = NavDestination.Faq },
                                    onNavigateTab = { tab ->
                                        currentTab = tab
                                        currentDestination = NavDestination.Main(tab)
                                    },
                                    onNavigateCheckInScanner = { currentDestination = NavDestination.MemberCheckInScanner },
                                    onNavigateGymExplorer = { currentDestination = NavDestination.GymExplorer },
                                    onNavigateClasses = { currentDestination = NavDestination.ClassReservations },
                                    onNavigateProgress = { currentDestination = NavDestination.TrainingProgress }
                                )
                                MainTab.MEMBERSHIP -> if (!isStaff) MembershipScreen(
                                    viewModel = membershipViewModel,
                                    userRoles = userRoles
                                )
                                MainTab.TRAINING -> if (!isStaff) TrainingHubScreen(
                                    accountId = accountId,
                                    repository = AppContainer.enterpriseTrainingRepository,
                                    onOpenJournal = { currentDestination = NavDestination.Journal },
                                    onOpenExercises = { currentDestination = NavDestination.ExerciseCatalog },
                                    onOpenProgress = { currentDestination = NavDestination.TrainingProgress },
                                    onOpenClasses = { currentDestination = NavDestination.ClassReservations }
                                )
                                MainTab.CATALOG -> CatalogScreen(
                                    catalogViewModel = catalogViewModel,
                                    favoritesViewModel = favoritesViewModel,
                                    receptionWhatsAppNumber = AppContainer.configRepository.receptionWhatsAppNumber,
                                    canPurchase = userRoles.contains("CLIENTE"),
                                    cartCount = shoppingCart.totalQuantity,
                                    cartError = cartError,
                                    isRefreshingCart = refreshingOrders,
                                    onRetryCart = { coroutineScope.launch { refreshOrders() } },
                                    onOpenCart = { currentDestination = NavDestination.Cart },
                                    onAddToCart = { product ->
                                        coroutineScope.launch {
                                            val quantity = shoppingCart.items.firstOrNull { it.product.id == product.id }?.quantity ?: 0
                                            orderApi.setQuantity(product.id, quantity + 1).onSuccess { shoppingCart = it; cartError = null }
                                                .onFailure { cartError = it.message }
                                        }
                                    }
                                )
                                MainTab.INBOX -> InboxScreen(viewModel = inboxViewModel)
                                MainTab.PROFILE -> profileViewModel?.let {
                                    ProfileScreen(
                                        viewModel = it,
                                        userRoles = userRoles,
                                        onNavigateAdminScanner = { currentDestination = NavDestination.AdminScanner }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
