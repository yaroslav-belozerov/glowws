package com.yaabelozerov.glowws.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.yaabelozerov.glowws.R

enum class Nav(
    val route: String,
    val resId: Int?,
    val iconActive: ImageVector? = null,
    val iconInactive: ImageVector? = null
) {
    MainScreenRoute(
        route = "MainScreen",
        resId = R.string.app_name,
        iconActive = Icons.Filled.Home,
        iconInactive = Icons.Outlined.Home
    ),
    IdeaScreenRoute(route = "IdeaScreen", resId = null), ArchiveScreenRoute(
        route = "ArchiveScreen",
        resId = R.string.a_screen_name,
        iconActive = Icons.Filled.Archive,
        iconInactive = Icons.Outlined.Archive
    ),
    SettingsScreenRoute(
        route = "SettingsScreen",
        resId = R.string.s_screen_name,
        iconActive = Icons.Filled.Settings,
        iconInactive = Icons.Outlined.Settings
    ),
    AiScreenRoute(
        route = "AiScreen", resId = R.string.s_cat_ai
    ),
    FeedbackRoute(route = "FeedbackScreen", resId = R.string.s_cat_feedback)
}

fun Nav.withParam(param: Any): String {
    return "$route/$param"
}

fun String.toDestination() = Nav.entries.find { it.route == this }
