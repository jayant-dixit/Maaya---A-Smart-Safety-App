package org.maaya.app.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Onboarding : Screen("onboarding", "Setup")
    object Dashboard : Screen("dashboard", "Maaya Shield")
    object ActiveSos : Screen("active_sos", "Emergency SOS")
    object TrustedContacts : Screen("contacts", "Trusted Contacts")
    object Smartwatch : Screen("smartwatch", "Wearable Health")
    object SafeRide : Screen("safe_ride", "Safe Ride")
    object IncidentHistory : Screen("history", "Incident History")
    object Settings : Screen("settings", "Security & Settings")
}
