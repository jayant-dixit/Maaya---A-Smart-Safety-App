package org.maaya.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.maaya.app.di.AppContainer
import org.maaya.app.ui.screens.contacts.TrustedContactsScreen
import org.maaya.app.ui.screens.dashboard.DashboardScreen
import org.maaya.app.ui.screens.history.IncidentHistoryScreen
import org.maaya.app.ui.screens.onboarding.OnboardingScreen
import org.maaya.app.ui.screens.saferide.SafeRideScreen
import org.maaya.app.ui.screens.settings.SettingsScreen
import org.maaya.app.ui.screens.smartwatch.SmartwatchScreen
import org.maaya.app.ui.screens.sos.ActiveSosScreen

@Composable
fun MaayaNavGraph(
    container: AppContainer,
    navController: NavHostController = rememberNavController()
) {
    val startDestination = if (container.securePreferences.isOnboardingCompleted) {
        Screen.Dashboard.route
    } else {
        Screen.Onboarding.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                securePreferences = container.securePreferences,
                onComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                engine = container.emergencySosEngine,
                bleRepository = container.bleHealthRepository,
                contactRepository = container.contactRepository,
                batteryMonitor = container.batteryMonitor,
                securePreferences = container.securePreferences,
                onNavigateToContacts = { navController.navigate(Screen.TrustedContacts.route) },
                onNavigateToSmartwatch = { navController.navigate(Screen.Smartwatch.route) },
                onNavigateToSafeRide = { navController.navigate(Screen.SafeRide.route) },
                onNavigateToHistory = { navController.navigate(Screen.IncidentHistory.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToActiveSos = { navController.navigate(Screen.ActiveSos.route) }
            )
        }

        composable(Screen.ActiveSos.route) {
            ActiveSosScreen(
                engine = container.emergencySosEngine,
                mediaProvider = container.mediaProvider,
                onResolved = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.ActiveSos.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.TrustedContacts.route) {
            TrustedContactsScreen(
                contactRepository = container.contactRepository,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Smartwatch.route) {
            SmartwatchScreen(
                bleRepository = container.bleHealthRepository,
                engine = container.emergencySosEngine,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SafeRide.route) {
            SafeRideScreen(
                safeRideRepository = container.safeRideRepository,
                locationTracker = container.locationTracker,
                safeRideMonitor = container.safeRideMonitor,
                engine = container.emergencySosEngine,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.IncidentHistory.route) {
            IncidentHistoryScreen(
                incidentRepository = container.incidentRepository,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                securePreferences = container.securePreferences,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
