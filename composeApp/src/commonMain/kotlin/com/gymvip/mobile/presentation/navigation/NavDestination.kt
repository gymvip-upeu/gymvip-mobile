package com.gymvip.mobile.presentation.navigation

enum class MainTab(val title: String) {
    HOME("Inicio"),
    MEMBERSHIP("Membresía"),
    CATALOG("Catálogo"),
    INBOX("Avisos"),
    PROFILE("Perfil")
}

sealed class NavDestination {
    data object Splash : NavDestination()
    data object Onboarding : NavDestination()
    data object Login : NavDestination()
    data object PasswordRecovery : NavDestination()
    data object Activation : NavDestination()
    data class Main(val selectedTab: MainTab = MainTab.HOME) : NavDestination()
    data object Faq : NavDestination()
    data object Preferences : NavDestination()
    data object Services : NavDestination()
    data object AdminScanner : NavDestination()
    data class TermsAndPrivacy(val requiresAcceptance: Boolean = false) : NavDestination()
}