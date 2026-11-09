package com.gymvip.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.gymvip.mobile.di.AppContainer
import com.gymvip.mobile.presentation.component.VipDrawerContent
import com.gymvip.mobile.presentation.component.VipNavigationBar
import com.gymvip.mobile.presentation.component.VipTopAppBar
import com.gymvip.mobile.presentation.navigation.MainTab
import com.gymvip.mobile.presentation.navigation.NavDestination
import com.gymvip.mobile.presentation.screen.auth.ActivationScreen
import com.gymvip.mobile.presentation.screen.auth.LoginScreen
import com.gymvip.mobile.presentation.screen.auth.PasswordRecoveryScreen
import com.gymvip.mobile.presentation.screen.catalog.AdminProductScannerScreen
import com.gymvip.mobile.presentation.screen.catalog.CatalogScreen
import com.gymvip.mobile.presentation.screen.faq.FaqScreen
import com.gymvip.mobile.presentation.screen.home.HomeScreen
import com.gymvip.mobile.presentation.screen.inbox.InboxScreen
import com.gymvip.mobile.presentation.screen.legal.TermsAndPrivacyScreen
import com.gymvip.mobile.presentation.screen.membership.MembershipScreen
import com.gymvip.mobile.presentation.screen.permissions.AppPermissionsScreen
import com.gymvip.mobile.presentation.screen.preferences.PreferencesScreen
import com.gymvip.mobile.presentation.screen.profile.ProfileScreen
import com.gymvip.mobile.presentation.screen.services.ServicesScreen
import com.gymvip.mobile.presentation.screen.splash.OnboardingScreen
import com.gymvip.mobile.presentation.screen.splash.SplashScreen
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
fun App() {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Preferencias de tema
    val preferencesViewModel = remember {
        PreferencesViewModel(AppContainer.configRepository)
    }
    val preferencesState by preferencesViewModel.uiState.collectAsState()

    GymVipTheme(themePreference = preferencesState.currentTheme) {
        val authViewModel = remember {
            AuthViewModel(
                loginUseCase = AppContainer.loginUseCase,
                activateAccountUseCase = AppContainer.activateAccountUseCase,
                authRepository = AppContainer.authRepository
            )
        }
        val authState by authViewModel.uiState.collectAsState()

        var currentDestination by remember { mutableStateOf<NavDestination>(NavDestination.Splash) }
        var currentTab by remember { mutableStateOf(MainTab.HOME) }

        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

        // Una sesión autenticada no entra al contenido hasta aceptar términos y privacidad.
        LaunchedEffect(authState.isAuthenticated, authState.authenticatedProfile?.accountId) {
            if (authState.isAuthenticated) {
                val accountId = authState.authenticatedProfile?.accountId
                if (accountId.isNullOrBlank()) {
                    currentDestination = NavDestination.Login
                    return@LaunchedEffect
                }

                val consent = AppContainer.legalConsentRepository.fetchConsent(accountId).getOrNull()
                val hasRequiredConsent = consent?.let {
                    it.termsAccepted && it.privacyAcknowledged && it.documentVersion == "v1.0"
                } ?: false

                if (!hasRequiredConsent) {
                    currentDestination = NavDestination.TermsAndPrivacy(requiresAcceptance = true)
                } else {
                    when (val destination = currentDestination) {
                        is NavDestination.Splash,
                        is NavDestination.Onboarding,
                        is NavDestination.Login,
                        is NavDestination.Activation,
                        is NavDestination.PasswordRecovery -> currentDestination = NavDestination.Main(currentTab)
                        is NavDestination.TermsAndPrivacy -> {
                            if (destination.requiresAcceptance) currentDestination = NavDestination.Main(currentTab)
                        }
                        else -> Unit
                    }
                }
            } else if (
                currentDestination !is NavDestination.Splash &&
                currentDestination !is NavDestination.Onboarding &&
                currentDestination !is NavDestination.Login &&
                currentDestination !is NavDestination.PasswordRecovery &&
                currentDestination !is NavDestination.Activation
            ) {
                currentDestination = NavDestination.Onboarding
            }
        }

        when (val dest = currentDestination) {
            is NavDestination.Splash -> {
                SplashScreen(
                    onResolved = { isActive ->
                        if (!isActive) currentDestination = NavDestination.Onboarding
                    },
                    isSessionActive = authState.isAuthenticated
                )
            }

            is NavDestination.Onboarding -> {
                OnboardingScreen(
                    onLogin = { currentDestination = NavDestination.Login },
                    onActivate = { currentDestination = NavDestination.Activation }
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

            is NavDestination.Activation -> {
                ActivationScreen(
                    viewModel = authViewModel,
                    onBackToLogin = { currentDestination = NavDestination.Login }
                )
            }

            is NavDestination.Faq -> {
                FaqScreen(
                    getFaqsUseCase = AppContainer.getFaqsUseCase,
                    onBack = { currentDestination = NavDestination.Main(currentTab) }
                )
            }

            is NavDestination.Services -> {
                ServicesScreen(
                    repository = AppContainer.gymServiceRepository,
                    onBack = { currentDestination = NavDestination.Main(currentTab) }
                )
            }

            is NavDestination.TermsAndPrivacy -> {
                val accountId = authState.authenticatedProfile?.accountId ?: "unknown_account"
                TermsAndPrivacyScreen(
                    repository = AppContainer.legalConsentRepository,
                    accountId = accountId,
                    requiredAcceptance = dest.requiresAcceptance,
                    onBackOrAccepted = { currentDestination = NavDestination.Main(currentTab) }
                )
            }

            is NavDestination.Preferences -> {
                PreferencesScreen(
                    viewModel = preferencesViewModel,
                    onBack = { currentDestination = NavDestination.Main(currentTab) }
                )
            }

            is NavDestination.Permissions -> {
                AppPermissionsScreen(
                    onBack = { currentDestination = NavDestination.Main(currentTab) }
                )
            }

            is NavDestination.Main -> {
                val accountId = authState.authenticatedProfile?.accountId ?: "unknown_account"

                // ViewModels para la sesión del socio
                val homeViewModel = remember(accountId) {
                    HomeViewModel(
                        memberRepository = AppContainer.memberRepository,
                        getMembershipSummaryUseCase = AppContainer.getMembershipSummaryUseCase,
                        configRepository = AppContainer.configRepository,
                        accountId = accountId
                    )
                }

                val membershipViewModel = remember(accountId) {
                    MembershipViewModel(
                        getMembershipSummaryUseCase = AppContainer.getMembershipSummaryUseCase,
                        getPaymentHistoryUseCase = AppContainer.getPaymentHistoryUseCase,
                        accountId = accountId
                    )
                }

                val catalogViewModel = remember {
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

                val profileViewModel = remember(accountId) {
                    ProfileViewModel(
                        memberRepository = AppContainer.memberRepository,
                        authRepository = AppContainer.authRepository,
                        accountId = accountId
                    )
                }

                val userRoles = authState.authenticatedProfile?.roles.orEmpty()
                val isAdminOrReception = userRoles.any {
                    it.contains("ADMIN", ignoreCase = true) || it.contains("RECEPCION", ignoreCase = true)
                }
                val accountType = when {
                    userRoles.any { it.contains("ADMIN", ignoreCase = true) } -> "Administrador"
                    userRoles.any { it.contains("RECEPCION", ignoreCase = true) } -> "Recepción"
                    else -> "Socio / miembro"
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        VipDrawerContent(
                            memberName = authState.authenticatedProfile?.fullName ?: "Socio VIP",
                            memberEmail = authState.authenticatedProfile?.email ?: "",
                            accountType = accountType,
                            isAdminOrReception = isAdminOrReception,
                            onNavigateFaq = {
                                coroutineScope.launch {
                                    drawerState.close()
                                    currentDestination = NavDestination.Faq
                                }
                            },
                            onNavigateServices = {
                                coroutineScope.launch {
                                    drawerState.close()
                                    currentDestination = NavDestination.Services
                                }
                            },
                            onNavigateTermsAndPrivacy = {
                                coroutineScope.launch {
                                    drawerState.close()
                                    currentDestination = NavDestination.TermsAndPrivacy()
                                }
                            },
                            onNavigatePreferences = {
                                coroutineScope.launch {
                                    drawerState.close()
                                    currentDestination = NavDestination.Preferences
                                }
                            },
                            onNavigatePermissions = {
                                coroutineScope.launch {
                                    drawerState.close()
                                    currentDestination = NavDestination.Permissions
                                }
                            },
                            onNavigateAdminScanner = {
                                coroutineScope.launch {
                                    drawerState.close()
                                    currentDestination = NavDestination.AdminScanner
                                }
                            },
                            onSyncNow = {
                                coroutineScope.launch {
                                    drawerState.close()
                                    favoritesViewModel.syncPending()
                                    membershipViewModel.refresh()
                                    homeViewModel.refreshAll()
                                    snackbarHostState.showSnackbar("Sincronización manual ejecutada")
                                }
                            },
                            onLogout = {
                                coroutineScope.launch {
                                    drawerState.close()
                                    authViewModel.logout()
                                }
                            }
                        )
                    }
                ) {
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
                                onOpenDrawer = {
                                    coroutineScope.launch { drawerState.open() }
                                },
                                onSyncClick = {
                                    favoritesViewModel.syncPending()
                                    membershipViewModel.refresh()
                                    homeViewModel.refreshAll()
                                }
                            )
                        },
                        bottomBar = {
                            VipNavigationBar(
                                currentTab = currentTab,
                                onTabSelected = { tab ->
                                    currentTab = tab
                                    currentDestination = NavDestination.Main(tab)
                                }
                            )
                        },
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentTab) {
                                MainTab.HOME -> HomeScreen(
                                    viewModel = homeViewModel,
                                    onNavigateTab = { tab ->
                                        currentTab = tab
                                        currentDestination = NavDestination.Main(tab)
                                    }
                                )
                                MainTab.MEMBERSHIP -> MembershipScreen(viewModel = membershipViewModel)
                                MainTab.CATALOG -> CatalogScreen(
                                    catalogViewModel = catalogViewModel,
                                    favoritesViewModel = favoritesViewModel
                                )
                                MainTab.INBOX -> InboxScreen(viewModel = inboxViewModel)
                                MainTab.PROFILE -> ProfileScreen(
                                    viewModel = profileViewModel,
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
