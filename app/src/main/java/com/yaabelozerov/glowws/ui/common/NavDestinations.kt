package com.yaabelozerov.glowws.ui.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.yaabelozerov.glowws.R

enum class NavDestinations(
    val route: String,
    val resId: Int?,
    val iconActive: ImageVector? = null,
    val iconInactive: ImageVector? = null
) {
    MainScreenRoute(
        "MainScreen",
        R.string.app_name,
        Icons.Filled.Home,
        Icons.Outlined.Home
    ),
    IdeaScreenRoute("IdeaScreen", null), ArchiveScreenRoute(
        "ArchiveScreen",
        R.string.a_screen_name,
        Icons.Filled.Archive,
        Icons.Outlined.Archive
    ),
    SettingsScreenRoute(
        "SettingsScreen",
        R.string.s_screen_name,
        Icons.Filled.Settings,
        Icons.Outlined.Settings
    ),
    AiScreenRoute("AiScreen", R.string.s_cat_ai),
    FeedbackRoute("FeedbackScreen", R.string.s_cat_feedback)
}

fun NavDestinations.withParam(param: Any): String {
    return "$route/$param"
}

fun String.toDestination() = NavDestinations.entries.find { it.route == this }
