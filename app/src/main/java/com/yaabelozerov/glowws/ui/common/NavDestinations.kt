package com.yaabelozerov.glowws.ui.common

sealed class NavDestinations(val route: String) {
    data object MainScreenRoute : NavDestinations("MainScreen")
    data object IdeaScreenRoute : NavDestinations("IdeaScreen")
    data object ArchiveScreenRoute : NavDestinations("ArchiveScreen")
    data object SettingsScreenRoute : NavDestinations("SettingsScreen")
    data object AiScreenRoute : NavDestinations("AiScreen")
}

fun NavDestinations.withParam(param: Any): String {
    return "$route/$param"
}
