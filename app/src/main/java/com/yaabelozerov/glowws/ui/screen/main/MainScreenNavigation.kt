package com.yaabelozerov.glowws.ui.screen.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yaabelozerov.glowws.ui.screen.archive.ArchiveScreen
import com.yaabelozerov.glowws.ui.screen.archive.ArchiveScreenViewModel
import com.yaabelozerov.glowws.ui.screen.idea.IdeaScreen
import com.yaabelozerov.glowws.ui.screen.idea.IdeaScreenViewModel
import com.yaabelozerov.glowws.ui.screen.settings.SettingsScreen
import com.yaabelozerov.glowws.ui.screen.settings.SettingsScreenViewModel

sealed class NavDestinations(val route: String) {
    data object MainScreenRoute : NavDestinations("MainScreen")
    data object IdeaScreenRoute : NavDestinations("IdeaScreen")
    data object ArchiveScreenRoute : NavDestinations("ArchiveScreen")
    data object SettingsScreenRoute : NavDestinations("SettingsScreen")
}

fun NavDestinations.withParam(param: Any): String {
    return "$route/$param"
}

@Composable
fun MainScreenNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: NavDestinations,
    mvm: MainScreenViewModel,
    ivm: IdeaScreenViewModel,
    svm: SettingsScreenViewModel,
    avm: ArchiveScreenViewModel
) {
    NavHost(navController = navController, startDestination = startDestination.route) {
        composable(NavDestinations.MainScreenRoute.route) {
            Column(Modifier.then(modifier)) {
                TitleBar(onSettings = { navController.navigate(NavDestinations.SettingsScreenRoute.route) },
                    onArchive = { navController.navigate(NavDestinations.ArchiveScreenRoute.route) })
                MainScreen(
                    ideas = mvm.state.collectAsState().value.ideas,
                    onSaveProject = { id, text -> mvm.modifyGroupName(id, text) },
                    onArchiveProject = { id -> mvm.archiveGroup(id) },
                    onClickIdea = { id ->
                        navController.navigate(NavDestinations.IdeaScreenRoute.withParam(id))
                        ivm.refreshPoints(id)
                    },
                    onAddIdeaToGroup = { groupId ->
                        mvm.addIdeaToGroup("", groupId, callback = { id ->
                            navController.navigate(NavDestinations.IdeaScreenRoute.withParam(id))
                            ivm.refreshPoints(id)
                        })
                    },
                    onArchiveIdea = { id -> mvm.archiveIdea(id) },
                    onSelect = { id -> mvm.onSelect(id) },
                    inSelectionMode = mvm.selection.collectAsState().value.inSelectionMode,
                    selection = mvm.selection.collectAsState().value.entries,
                    settings = svm.state.collectAsState().value.map { it.value }.flatten()
                )
            }
        }
        composable(
            NavDestinations.IdeaScreenRoute.withParam("{id}"), arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            IdeaScreen(modifier = Modifier.then(modifier),
                points = ivm.points.collectAsState().value,
                onBack = {
                    navController.navigateUp()
                },
                onAdd = { ind ->
                    ivm.addPointAtIndex(
                        backStackEntry.arguments!!.getLong("id"), ind
                    )
                },
                onSave = { pointId, newText, isMain ->
                    ivm.modifyPoint(
                        pointId, newText, isMain
                    )
                },
                onRemove = { pointId ->
                    ivm.removePoint(pointId)
                })
        }
        composable(
            NavDestinations.SettingsScreenRoute.route
        ) {
            SettingsScreen(modifier = Modifier.then(modifier),
                svm.state.collectAsState().value,
                onModify = { key, value ->
                    svm.modifySetting(key, value) { mvm.fetchSortFilter() }
                })
        }
        composable(
            NavDestinations.ArchiveScreenRoute.route
        ) {
            ArchiveScreen(
                modifier = Modifier.then(modifier),
                ideas = avm.state.collectAsState().value,
                onClick = { id ->
                    navController.navigate(NavDestinations.IdeaScreenRoute.withParam(id))
                    ivm.refreshPoints(id)
                },
                onRemove = { id -> avm.removeIdea(id) },
                onUnarchive = { id -> avm.unarchiveIdea(id) },
                onSelect = { id -> avm.onSelect(id) },
                selection = avm.selection.collectAsState().value
            )
        }
    }
}