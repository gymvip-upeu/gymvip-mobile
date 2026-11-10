package pe.edu.upeu.gymvip

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.runtime.saveable.rememberSaveable
import pe.edu.upeu.gymvip.presentation.navigation.destinoSaver
import pe.edu.upeu.gymvip.presentation.navigation.AlVolver
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import pe.edu.upeu.gymvip.di.AppContainer
import pe.edu.upeu.gymvip.presentation.component.VipDrawerContent
import pe.edu.upeu.gymvip.presentation.component.VipNavigationBar
import pe.edu.upeu.gymvip.presentation.component.VipTopAppBar
import pe.edu.upeu.gymvip.presentation.navigation.MainTab
import pe.edu.upeu.gymvip.presentation.navigation.NavDestination
import pe.edu.upeu.gymvip.presentation.screen.auth.ActivationScreen
import pe.edu.upeu.gymvip.presentation.screen.auth.LoginScreen
import pe.edu.upeu.gymvip.presentation.screen.auth.OnboardingScreen
import pe.edu.upeu.gymvip.presentation.screen.personal.PersonalScreen
import pe.edu.upeu.gymvip.presentation.component.LoadingView
import pe.edu.upeu.gymvip.presentation.screen.catalog.CatalogoComunidadScreen
import pe.edu.upeu.gymvip.presentation.screen.faq.AyudaComunidadScreen
import pe.edu.upeu.gymvip.presentation.screen.home.HomeScreen
import pe.edu.upeu.gymvip.presentation.screen.inbox.AvisosComunidadScreen
import pe.edu.upeu.gymvip.presentation.screen.legal.TermsAndPrivacyScreen
import pe.edu.upeu.gymvip.presentation.screen.membership.MembershipScreen
import pe.edu.upeu.gymvip.presentation.screen.membership.PaymentsScreen
import pe.edu.upeu.gymvip.presentation.screen.preferences.PreferencesScreen
import pe.edu.upeu.gymvip.presentation.screen.profile.ProfileScreen
import pe.edu.upeu.gymvip.presentation.screen.services.ServicesScreen
import pe.edu.upeu.gymvip.presentation.screen.splash.SplashScreen
import pe.edu.upeu.gymvip.presentation.theme.GymVipTheme
import pe.edu.upeu.gymvip.presentation.viewmodel.AuthViewModel
import pe.edu.upeu.gymvip.presentation.viewmodel.ComunidadViewModel
import pe.edu.upeu.gymvip.presentation.viewmodel.ComunidadUiState
import pe.edu.upeu.gymvip.presentation.viewmodel.SocioViewModel
import pe.edu.upeu.gymvip.presentation.viewmodel.PreferencesViewModel
import kotlinx.coroutines.launch

private fun NavDestination.esAcceso() = this is NavDestination.Splash || this is NavDestination.Onboarding ||
    this is NavDestination.Login || this is NavDestination.Activation

@Composable
fun App(onThemeChanged: (Boolean) -> Unit = {}) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Preferencias de tema
    val preferencesViewModel = remember {
        PreferencesViewModel(AppContainer.configRepository)
    }
    val preferencesState by preferencesViewModel.uiState.collectAsState()

    GymVipTheme(themePreference = preferencesState.currentTheme, onThemeChanged = onThemeChanged) {
        val authViewModel = remember {
            AuthViewModel(
                loginUseCase = AppContainer.loginUseCase,
                activateAccountUseCase = AppContainer.activateAccountUseCase,
                authRepository = AppContainer.authRepository
            )
        }
        val authState by authViewModel.uiState.collectAsState()
        DisposableEffect(authViewModel, preferencesViewModel) {
            onDispose { authViewModel.onCleared(); preferencesViewModel.onCleared() }
        }

        // El panel de personal no crea ViewModels ni consulta rutas privadas de socios.
        if (authState.sessionChecked && authState.isAuthenticated && authState.staffUser != null) {
            PersonalScreen(authState.staffUser!!, authViewModel::logout)
            return@GymVipTheme
        }

        val socioId = authState.authenticatedProfile?.accountId?.takeIf { authState.isAuthenticated && authState.sessionChecked }
        val comunidadViewModel = if (socioId != null) remember(socioId) { ComunidadViewModel(AppContainer.comunidadRepository, true) } else null
        DisposableEffect(comunidadViewModel) { onDispose { comunidadViewModel?.onCleared() } }
        val comunidadState = comunidadViewModel?.state?.collectAsState()?.value ?: ComunidadUiState()
        LaunchedEffect(comunidadState.errorAccion) {
            comunidadState.errorAccion?.let { snackbarHostState.showSnackbar(it); comunidadViewModel?.limpiarErrorAccion() }
        }

        var currentDestination by rememberSaveable(stateSaver = destinoSaver) { mutableStateOf<NavDestination>(NavDestination.Splash) }
        var currentTab by rememberSaveable { mutableStateOf(MainTab.HOME) }

        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        AlVolver(drawerState.isOpen || (currentDestination !is NavDestination.Splash && currentDestination !is NavDestination.Onboarding && currentDestination !is NavDestination.Login && currentDestination !is NavDestination.Main) || (authState.isAuthenticated && currentTab != MainTab.HOME)) {
            if (drawerState.isOpen) coroutineScope.launch { drawerState.close() }
            else if (currentDestination is NavDestination.Activation) { if (authState.mustChangePassword) authViewModel.logout(); currentDestination = NavDestination.Login }
            else if (currentDestination !is NavDestination.Main) currentDestination = NavDestination.Main(currentTab)
            else { currentTab = MainTab.HOME; currentDestination = NavDestination.Main(MainTab.HOME) }
        }

        // Manejo de eventos de autenticación
        LaunchedEffect(authState.sessionChecked, authState.isAuthenticated, authState.mustChangePassword) {
            if (!authState.sessionChecked) return@LaunchedEffect
            if (authState.mustChangePassword) currentDestination = NavDestination.Activation
            else if (authState.isAuthenticated) {
                if (currentDestination.esAcceso()) {
                    currentDestination = NavDestination.Main(currentTab)
                }
            } else if (!currentDestination.esAcceso() || currentDestination is NavDestination.Splash) {
                currentTab = MainTab.HOME
                currentDestination = if (authState.onboardingSeen) NavDestination.Login else NavDestination.Onboarding
            }
        }

        // Guardar una ruta nunca otorga acceso: esperar revalidación y primer cambio obligatorio.
        val destinoVisible = when {
            !authState.sessionChecked -> NavDestination.Splash
            authState.mustChangePassword -> NavDestination.Activation
            !authState.isAuthenticated && !currentDestination.esAcceso() -> NavDestination.Login
            else -> currentDestination
        }
        when (val dest = destinoVisible) {
            is NavDestination.Splash -> {
                LoadingView("Comprobando tu sesión…")
            }
            is NavDestination.Onboarding -> {
                OnboardingScreen(
                    login = { authViewModel.completeOnboarding { currentDestination = NavDestination.Login } },
                    activar = { authViewModel.completeOnboarding { currentDestination = NavDestination.Activation } }
                )
            }

            is NavDestination.Login -> {
                LoginScreen(
                    viewModel = authViewModel,
                    onNavigateToActivation = { currentDestination = NavDestination.Activation },
                    onBack = { currentDestination = NavDestination.Onboarding }
                )
            }

            is NavDestination.Activation -> {
                ActivationScreen(
                    viewModel = authViewModel,
                    onBackToLogin = { currentDestination = NavDestination.Login }
                )
            }

            is NavDestination.Faq -> {
                Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                    comunidadViewModel?.let { vm -> AyudaComunidadScreen(vm) { currentDestination = NavDestination.Main(currentTab) } }
                }
            }

            is NavDestination.Services -> {
                ServicesScreen(
                    repository = AppContainer.gymServiceRepository,
                    onBack = { currentDestination = NavDestination.Main(currentTab) }
                )
            }

            is NavDestination.TermsAndPrivacy -> {
                pe.edu.upeu.gymvip.presentation.screen.auth.TerminosActivacion {
                    currentDestination = NavDestination.Main(currentTab)
                }
            }

            is NavDestination.Preferences -> {
                Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                    PreferencesScreen(preferencesViewModel) { currentDestination = NavDestination.Main(currentTab) }
                }
            }

            is NavDestination.Main -> {
                val accountId = authState.authenticatedProfile?.accountId ?: "unknown_account"

                val socioViewModel = remember(accountId) { SocioViewModel(AppContainer.memberRepository, accountId) }
                var mostrarPagos by rememberSaveable(accountId) { mutableStateOf(false) }
                AlVolver(mostrarPagos) { mostrarPagos = false }

                val comunidadVm = requireNotNull(comunidadViewModel)
                val socioState by socioViewModel.state.collectAsState()
                DisposableEffect(socioViewModel) { onDispose { socioViewModel.onCleared() } }
                LaunchedEffect(currentTab) { if (currentTab == MainTab.PROFILE) socioViewModel.refresh() }

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
                            onNavigateServices = {
                                coroutineScope.launch {
                                    drawerState.close()
                                    currentDestination = NavDestination.Services
                                }
                            },
                            onNavigateTermsAndPrivacy = {
                                coroutineScope.launch {
                                    drawerState.close()
                                    currentDestination = NavDestination.TermsAndPrivacy
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
                                    socioViewModel.refresh()
                                    comunidadVm.cargarAvisos()
                                    comunidadVm.cargarFavoritos()
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
                        bottomBar = {
                            VipNavigationBar(
                                currentTab = currentTab,
                                avisosSinLeer = comunidadState.datos.avisos.count { !it.isRead },
                                onTabSelected = { tab ->
                                    mostrarPagos = false
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
                            if (mostrarPagos) PaymentsScreen(socioViewModel) { mostrarPagos = false }
                            else when (currentTab) {
                                MainTab.HOME -> HomeScreen(
                                    viewModel = socioViewModel,
                                    mostrarAvisos = preferencesState.expiryPopups,
                                    promociones = comunidadState.destacadas,
                                    comercial = comunidadVm,
                                    onVerPromo = { comunidadVm.abrirOferta(it.id); currentTab = MainTab.CATALOG },
                                    onOpenPayments = { mostrarPagos = true },
                                    avisosSinLeer = comunidadState.datos.avisos.count { !it.isRead },
                                    onNavigateTab = { tab ->
                                        currentTab = tab
                                        currentDestination = NavDestination.Main(tab)
                                    }
                                )
                                MainTab.MEMBERSHIP -> MembershipScreen(socioViewModel, onOpenPayments = { mostrarPagos = true })
                                MainTab.CATALOG -> CatalogoComunidadScreen(comunidadVm)
                                MainTab.INBOX -> AvisosComunidadScreen(comunidadVm)
                                MainTab.PROFILE -> ProfileScreen(
                                    perfil = socioState.datos?.perfil ?: authState.authenticatedProfile,
                                    cargando = socioState.cargando, error = socioState.error, actualizar = socioViewModel::refresh,
                                    preferencias = { currentDestination = NavDestination.Preferences },
                                    ayuda = { currentDestination = NavDestination.Faq }, salir = authViewModel::logout)

                            }
                        }
                    }
                }
            }
        }
    }
}
