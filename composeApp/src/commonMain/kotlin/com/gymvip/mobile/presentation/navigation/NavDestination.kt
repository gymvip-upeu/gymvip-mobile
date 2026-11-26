package com.gymvip.mobile.presentation.navigation

enum class MainTab(val title: String) {
    HOME("Inicio"),
    MEMBERSHIP("Membresía"),
    TRAINING("Entreno"),
    CATALOG("Tienda"),
    INBOX("Avisos"),
    PROFILE("Perfil")
}

sealed class NavDestination {
    data object Splash : NavDestination()
    data object Onboarding : NavDestination()
    data object Login : NavDestination()
    data object PasswordRecovery : NavDestination()
    data object Activation : NavDestination()
    data object FirstPassword : NavDestination()
    data class Main(val selectedTab: MainTab = MainTab.HOME) : NavDestination()
    data object GymExplorer : NavDestination()
    data object ExerciseCatalog : NavDestination()
    data object TrainingProgress : NavDestination()
    data object ClassReservations : NavDestination()
    data object Faq : NavDestination()
    data object Settings : NavDestination()
    data object EditProfile : NavDestination()
    data object Preferences : NavDestination()
    data object Services : NavDestination()
    data object AdminScanner : NavDestination()
    data object Cart : NavDestination()
    data object MemberCheckInScanner : NavDestination()
    data object Permissions : NavDestination()
    data object PostLoginPermissions : NavDestination()
    data object AccountSecurity : NavDestination()
    data object ConfirmEmail : NavDestination()
    data class TermsAndPrivacy(val requiresAcceptance: Boolean = false) : NavDestination()
}
