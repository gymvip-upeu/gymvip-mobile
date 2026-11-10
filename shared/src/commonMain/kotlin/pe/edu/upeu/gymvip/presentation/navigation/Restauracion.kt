package pe.edu.upeu.gymvip.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver

val destinoSaver = Saver<NavDestination, String>(
    save = { destino -> when (destino) {
        NavDestination.Splash -> "splash"
        NavDestination.Onboarding -> "onboarding"
        NavDestination.Login -> "login"
        NavDestination.Activation -> "activation"
        NavDestination.Faq -> "faq"
        NavDestination.Preferences -> "preferences"
        NavDestination.Services -> "services"
        NavDestination.TermsAndPrivacy -> "terms"
        is NavDestination.Main -> "main:${destino.selectedTab.name}"
    } },
    restore = { valor -> when (valor) {
        "splash" -> NavDestination.Splash
        "onboarding" -> NavDestination.Onboarding
        "activation" -> NavDestination.Activation
        "faq" -> NavDestination.Faq
        "preferences" -> NavDestination.Preferences
        "services" -> NavDestination.Services
        "terms" -> NavDestination.TermsAndPrivacy
        else -> if (valor.startsWith("main:")) NavDestination.Main(MainTab.valueOf(valor.substringAfter(':'))) else NavDestination.Login
    } }
)

@Composable
expect fun AlVolver(habilitado: Boolean, accion: () -> Unit)
