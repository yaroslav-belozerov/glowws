package com.yaabelozerov.glowws.ui.screen.main

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yaabelozerov.glowws.ui.common.NavDestinations
import com.yaabelozerov.glowws.ui.common.withParam
import com.yaabelozerov.glowws.ui.screen.ai.AiScreen
import com.yaabelozerov.glowws.ui.screen.ai.AiScreenViewModel
import com.yaabelozerov.glowws.ui.screen.archive.ArchiveScreen
import com.yaabelozerov.glowws.ui.screen.archive.ArchiveScreenViewModel
import com.yaabelozerov.glowws.ui.screen.idea.IdeaScreen
import com.yaabelozerov.glowws.ui.screen.idea.IdeaScreenViewModel
import com.yaabelozerov.glowws.ui.screen.settings.SettingsScreen
import com.yaabelozerov.glowws.ui.screen.settings.SettingsScreenViewModel

@Composable
fun MainScreenNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: NavDestinations,
    mvm: MainScreenViewModel,
    ivm: IdeaScreenViewModel,
    svm: SettingsScreenViewModel,
    avm: ArchiveScreenViewModel,
    aivm: AiScreenViewModel
) {
    NavHost(
        navController = navController, startDestination = startDestination.route
    ) {
        composable(NavDestinations.MainScreenRoute.route) {
            Column(modifier) {
                TitleBar(tooltipState = mvm.tooltipBarState.collectAsState().value,
                    onSettings = {
                        navController.navigate(
                            NavDestinations.SettingsScreenRoute.route
                        )
                    },
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
                    settings = svm.state.collectAsState().value.values.flatten()
                )
            }
        }
        composable(
            route = NavDestinations.IdeaScreenRoute.withParam("{id}"),
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { backStackEntry ->
            IdeaScreen(modifier = modifier,
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
                },
                onExecute = { pointId, content ->
                    aivm.executeInto(content) { new -> ivm.modifyPoint(pointId, new) }
                },
                settings = svm.state.collectAsState().value.values.flatten(),
                aiAvailable = aivm.inferenceManager.model.collectAsState().value != null
            )
        }
        composable(NavDestinations.SettingsScreenRoute.route) {
            SettingsScreen(modifier = modifier,
                svm.state.collectAsState().value,
                onModify = { key, value ->
                    svm.modifySetting(key, value) { mvm.fetchSortFilter() }
                },
                onNavigateToAi = {
                    navController.navigate(NavDestinations.AiScreenRoute.route)
                })
        }
        composable(
            NavDestinations.ArchiveScreenRoute.route
        ) {
            ArchiveScreen(modifier = modifier,
                ideas = avm.state.collectAsState().value,
                onClick = { id ->
                    navController.navigate(NavDestinations.IdeaScreenRoute.withParam(id))
                    ivm.refreshPoints(id)
                },
                onRemove = { id -> avm.removeIdea(id) },
                onUnarchive = { id -> avm.unarchiveIdea(id) },
                onSelect = { id -> avm.onSelect(id) },
                selectionState = avm.selection.collectAsState().value
            )
        }
        composable(NavDestinations.AiScreenRoute.route) {
            AiScreen(
                modifier = modifier,
                models = aivm.models.collectAsState().value,
                onChoose = { name -> aivm.pickModel(name) },
                onDelete = { name -> aivm.removeModel(name) },
                onUnload = { aivm.unloadModel() },
                onAdd = { aivm.importModel() },
                onRefresh = { aivm.refresh() },
                status = aivm.inferenceManager.status.collectAsState().value,
                error = aivm.inferenceManager.error.collectAsState().value
            )
        }
    }
}
