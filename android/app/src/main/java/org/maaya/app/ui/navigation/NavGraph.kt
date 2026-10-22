package org.maaya.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.maaya.app.di.AppContainer
import org.maaya.app.domain.engine.SosEngineState
import org.maaya.app.ui.components.MaayaBottomNavBar
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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val engineState by container.emergencySosEngine.engineState.collectAsState()
    LaunchedEffect(engineState) {
        if (engineState is SosEngineState.Active && currentRoute != Screen.ActiveSos.route) {
            navController.navigate(Screen.ActiveSos.route) {
                launchSingleTop = true
            }
        }
    }

    // The footer navigation bar is fixed to all 4 primary pages:
    // Home (Dashboard), Cab Escort (Safe Ride), Incident Log (History), Contacts (Trusted Contacts)
    val bottomBarRoutes = setOf(
        Screen.Dashboard.route,
        Screen.SafeRide.route,
        Screen.IncidentHistory.route,
        Screen.TrustedContacts.route
    )
    val showBottomBar = currentRoute in bottomBarRoutes

    val navigateToTab: (String) -> Unit = { targetRoute ->
        if (currentRoute != targetRoute) {
            if (targetRoute == Screen.Dashboard.route) {
                val popped = navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                if (!popped) {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            } else {
                navController.navigate(targetRoute) {
                    popUpTo(Screen.Dashboard.route) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
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
                    onNavigateToContacts = { navigateToTab(Screen.TrustedContacts.route) },
                    onNavigateToSmartwatch = { navController.navigate(Screen.Smartwatch.route) },
                    onNavigateToSafeRide = { navigateToTab(Screen.SafeRide.route) },
                    onNavigateToHistory = { navigateToTab(Screen.IncidentHistory.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToActiveSos = { navController.navigate(Screen.ActiveSos.route) }
                )
            }

            composable(Screen.ActiveSos.route) {
                ActiveSosScreen(
                    engine = container.emergencySosEngine,
                    mediaProvider = container.mediaProvider,
                    contactRepository = container.contactRepository,
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

        if (showBottomBar) {
            MaayaBottomNavBar(
                selectedRoute = when (currentRoute) {
                    Screen.Dashboard.route -> "home"
                    Screen.SafeRide.route -> "safe_ride"
                    Screen.IncidentHistory.route -> "history"
                    Screen.TrustedContacts.route -> "contacts"
                    else -> "home"
                },
                onHomeClick = { navigateToTab(Screen.Dashboard.route) },
                onSafeRideClick = { navigateToTab(Screen.SafeRide.route) },
                onHistoryClick = { navigateToTab(Screen.IncidentHistory.route) },
                onContactsClick = { navigateToTab(Screen.TrustedContacts.route) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }
    }
}
