package com.yaabelozerov.glowws.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
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
      iconInactive = Icons.Outlined.Home),
  IdeaScreenRoute(route = "IdeaScreen", resId = null),
  ArchiveScreenRoute(
      route = "ArchiveScreen",
      resId = R.string.a_screen_name,
      iconActive = Icons.Filled.Archive,
      iconInactive = Icons.Outlined.Archive),
  SettingsScreenRoute(
      route = "SettingsScreen",
      resId = R.string.s_screen_name,
      iconActive = Icons.Filled.Settings,
      iconInactive = Icons.Outlined.Settings),
  AiScreenRoute(route = "AiScreen", resId = R.string.s_cat_ai),
  FeedbackRoute(route = "FeedbackScreen", resId = R.string.s_cat_feedback)
}

fun Nav.withParam(param: Any): String {
  return "$route/$param"
}

fun String.toDestination() = Nav.entries.find { it.route == this }

@Composable
fun BottomNavBar(navController: NavHostController) {
  val items =
      listOf(
          Nav.SettingsScreenRoute,
          Nav.MainScreenRoute,
          Nav.ArchiveScreenRoute,
      )
  NavigationBar {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    items.forEach { screen ->
      val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
      NavigationBarItem(
          selected = selected,
          onClick = {
            navController.popBackStack(screen.route, inclusive = true, saveState = true)
            navController.navigate(screen.route)
          },
          icon = {
            Icon(
                imageVector = if (selected) screen.iconActive!! else screen.iconInactive!!,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = if (!selected) Modifier.alpha(0.4f) else Modifier)
          })
    }
  }
}
