package com.gymvip.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
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
        var shoppingCart by remember(profileAccountId) { mutableStateOf(ShoppingCart()) }
        var orders by remember(profileAccountId) { mutableStateOf<List<SupplementOrder>>(emptyList()) }
        var paymentLink by remember(profileAccountId) { mutableStateOf<DigitalPaymentLink?>(null) }
        var culqiEnabled by remember(profileAccountId) { mutableStateOf(false) }
        var cartError by remember(profileAccountId) { mutableStateOf<String?>(null) }
        var checkoutKey by remember(profileAccountId) { mutableStateOf<String?>(null) }
        val orderApi = AppContainer.supplementOrderApi
        suspend fun refreshOrders() {
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
            NavDestination.Faq, NavDestination.Services, NavDestination.MemberCheckInScanner -> NavDestination.Main(MainTab.HOME)
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
        LaunchedEffect(authState.isAuthenticated, authState.authenticatedProfile?.accountId) {
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

        when (val dest = currentDestination) {
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
                val isStaff = userRoles.any { it == "ADMIN" || it == "GERENTE" || it == "RECEPCION" || it == "AUDITOR" } && !userRoles.contains("CLIENTE")
                val isExecutive = userRoles.any { it == "ADMIN" || it == "GERENTE" }
                val visibleTabs = when {
                    userRoles.contains("CLIENTE") -> MainTab.entries
                    userRoles.contains("GERENTE") || userRoles.contains("ADMIN") -> listOf(MainTab.HOME, MainTab.CATALOG, MainTab.INBOX, MainTab.PROFILE)
                    userRoles.contains("AUDITOR") && !isExecutive -> listOf(MainTab.HOME, MainTab.PROFILE)
                    else -> listOf(MainTab.HOME, MainTab.CATALOG, MainTab.INBOX, MainTab.PROFILE)
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
                        isStaff = isStaff
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

                Scaffold(
                    topBar = {
                        VipTopAppBar(
                            title = when (currentTab) {
                                MainTab.HOME -> "GYM VIP"
                                MainTab.MEMBERSHIP -> "Membresía"
                                MainTab.CATALOG -> "Catálogo y Planes"
                                MainTab.INBOX -> "Avisos"
                                MainTab.PROFILE -> "Mi Perfil"
                            },
                            onOpenSettings = if (currentTab == MainTab.PROFILE) {
                                { currentDestination = NavDestination.Settings }
                            } else null,
                            onSyncClick = {
                                favoritesViewModel.syncPending()
                                membershipViewModel.refresh()
                                homeViewModel.refreshAll()
                                inboxViewModel.refresh()
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
                                onNavigateServices = { currentDestination = NavDestination.Services },
                                onNavigateFaq = { currentDestination = NavDestination.Faq },
                                onNavigateTab = { tab ->
                                    currentTab = tab
                                    currentDestination = NavDestination.Main(tab)
                                },
                                onNavigateCheckInScanner = { currentDestination = NavDestination.MemberCheckInScanner }
                            )
                            MainTab.MEMBERSHIP -> if (!isStaff) MembershipScreen(
                                viewModel = membershipViewModel,
                                userRoles = userRoles,
                                onNavigateToCatalog = {
                                    currentTab = MainTab.CATALOG
                                    currentDestination = NavDestination.Main(MainTab.CATALOG)
                                }
                            )
                            MainTab.CATALOG -> CatalogScreen(
                                catalogViewModel = catalogViewModel,
                                favoritesViewModel = favoritesViewModel,
                                receptionWhatsAppNumber = AppContainer.configRepository.receptionWhatsAppNumber,
                                canPurchase = userRoles.contains("CLIENTE"),
                                cartCount = shoppingCart.totalQuantity,
                                cartError = cartError,
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
