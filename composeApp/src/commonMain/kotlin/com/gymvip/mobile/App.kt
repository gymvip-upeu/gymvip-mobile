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
import com.gymvip.mobile.presentation.screen.catalog.CatalogScreen
import com.gymvip.mobile.presentation.screen.faq.FaqScreen
import com.gymvip.mobile.presentation.screen.home.HomeScreen
import com.gymvip.mobile.presentation.screen.inbox.InboxScreen
import com.gymvip.mobile.presentation.screen.membership.MembershipScreen
import com.gymvip.mobile.presentation.screen.preferences.PreferencesScreen
import com.gymvip.mobile.presentation.screen.profile.ProfileScreen
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

        // Manejo de eventos de autenticación
        LaunchedEffect(authState.isAuthenticated) {
            if (authState.isAuthenticated) {
                if (currentDestination is NavDestination.Login || currentDestination is NavDestination.Activation || currentDestination is NavDestination.Splash) {
                    currentDestination = NavDestination.Main(currentTab)
                }
            } else if (currentDestination !is NavDestination.Splash) {
                currentDestination = NavDestination.Login
            }
        }

        when (val dest = currentDestination) {
            is NavDestination.Splash -> {
                SplashScreen(
                    onResolved = { isActive ->
                        currentDestination = if (isActive) NavDestination.Main(MainTab.HOME) else NavDestination.Login
                    },
                    isSessionActive = authState.isAuthenticated
                )
            }

            is NavDestination.Login -> {
                LoginScreen(
                    viewModel = authViewModel,
                    onNavigateToActivation = { currentDestination = NavDestination.Activation }
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

            is NavDestination.Preferences -> {
                PreferencesScreen(
                    viewModel = preferencesViewModel,
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

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        VipDrawerContent(
                            memberName = authState.authenticatedProfile?.fullName ?: "Socio VIP",
                            memberEmail = authState.authenticatedProfile?.email ?: "",
                            onNavigateFaq = {
                                coroutineScope.launch {
                                    drawerState.close()
                                    currentDestination = NavDestination.Faq
                                }
                            },
                            onNavigatePreferences = {
                                coroutineScope.launch {
                                    drawerState.close()
                                    currentDestination = NavDestination.Preferences
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
                                MainTab.PROFILE -> ProfileScreen(viewModel = profileViewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
