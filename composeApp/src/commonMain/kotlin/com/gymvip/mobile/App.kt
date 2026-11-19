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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.gymvip.mobile.di.AppContainer
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
import com.gymvip.mobile.presentation.screen.faq.FaqScreen
import com.gymvip.mobile.presentation.screen.home.HomeScreen
import com.gymvip.mobile.presentation.screen.inbox.InboxScreen
import com.gymvip.mobile.presentation.screen.legal.TermsAndPrivacyScreen
import com.gymvip.mobile.presentation.screen.membership.MembershipScreen
import com.gymvip.mobile.presentation.screen.permissions.AppPermissionsScreen
import com.gymvip.mobile.presentation.screen.preferences.PreferencesScreen
import com.gymvip.mobile.presentation.screen.profile.EditProfileScreen
import com.gymvip.mobile.presentation.screen.profile.ProfileScreen
import com.gymvip.mobile.presentation.screen.settings.SettingsScreen
import com.gymvip.mobile.presentation.screen.settings.AccountSecurityScreen
import com.gymvip.mobile.presentation.screen.settings.ConfirmEmailScreen
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

@Composable
fun App(onDarkThemeChanged: ((Boolean) -> Unit)? = null) {
    // Preferencias de tema
    val preferencesViewModel = remember {
        PreferencesViewModel(AppContainer.configRepository)
    }
    val preferencesState by preferencesViewModel.uiState.collectAsState()

    GymVipTheme(themePreference = preferencesState.currentTheme, onDarkThemeChanged = onDarkThemeChanged) {
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

        val profileAccountId = authState.authenticatedProfile?.accountId?.takeIf { it.isNotBlank() }
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
            NavDestination.Faq, NavDestination.Services -> NavDestination.Main(MainTab.HOME)
            NavDestination.EditProfile, NavDestination.Preferences, NavDestination.Permissions,
            NavDestination.AccountSecurity -> NavDestination.Settings
            NavDestination.ConfirmEmail -> NavDestination.AccountSecurity
            is NavDestination.TermsAndPrivacy -> if (destination.requiresAcceptance) null else NavDestination.Settings
            else -> null
        }
        AppBackHandler(enabled = parentDestination != null) {
            parentDestination?.let { currentDestination = it }
        }

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
                currentTab = MainTab.HOME
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
                    onAccountSecurity = { currentDestination = NavDestination.AccountSecurity },
                    onPermissions = { currentDestination = NavDestination.Permissions },
                    onAppearance = { currentDestination = NavDestination.Preferences },
                    onTermsAndPrivacy = { currentDestination = NavDestination.TermsAndPrivacy() },
                    onLogout = authViewModel::logout
                )
            }

            is NavDestination.EditProfile -> {
                profileViewModel?.let {
                    EditProfileScreen(viewModel = it, onBack = { currentDestination = NavDestination.Settings })
                }
            }

            is NavDestination.AccountSecurity -> {
                profileViewModel?.let {
                    AccountSecurityScreen(
                        viewModel = it,
                        onBack = { currentDestination = NavDestination.Settings },
                        onChangePassword = { currentDestination = NavDestination.ConfirmEmail }
                    )
                }
            }

            is NavDestination.ConfirmEmail -> {
                ConfirmEmailScreen(
                    viewModel = authViewModel,
                    onBack = { currentDestination = NavDestination.AccountSecurity }
                )
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
                DisposableEffect(inboxViewModel) {
                    onDispose { inboxViewModel.onCleared() }
                }

                val userRoles = authState.authenticatedProfile?.roles.orEmpty()

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
                            onTabSelected = { tab ->
                                currentTab = tab
                                currentDestination = NavDestination.Main(tab)
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
                                onNavigateServices = { currentDestination = NavDestination.Services },
                                onNavigateFaq = { currentDestination = NavDestination.Faq },
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
