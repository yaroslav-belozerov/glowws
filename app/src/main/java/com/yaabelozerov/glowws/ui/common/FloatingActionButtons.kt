package com.yaabelozerov.glowws.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.yaabelozerov.glowws.ui.screen.archive.ArchiveScreenFloatingButtons
import com.yaabelozerov.glowws.ui.screen.idea.IdeaScreenViewModel
import com.yaabelozerov.glowws.ui.screen.main.MainScreenFloatingButtons
import com.yaabelozerov.glowws.ui.screen.main.MainScreenViewModel

@Composable
fun FloatingActionButtons(
    navController: NavHostController,
    mvm: MainScreenViewModel,
    ivm: IdeaScreenViewModel,
) {
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  when (navBackStackEntry?.destination?.route?.toDestination()) {
    Nav.MainScreenRoute ->
        MainScreenFloatingButtons(
            modifier = Modifier,
            mvm = mvm,
            addNewIdeaCallback = { id ->
              navController.navigate(Nav.IdeaScreenRoute.withParam(id))
              ivm.refreshPoints(id)
            })

    Nav.ArchiveScreenRoute -> ArchiveScreenFloatingButtons(mvm = mvm)

    else -> {}
  }
}
