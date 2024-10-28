package com.yaabelozerov.glowws.ui.screen.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.data.local.ai.InferenceManagerState
import com.yaabelozerov.glowws.ui.common.NavDestinations
import com.yaabelozerov.glowws.ui.common.withParam
import com.yaabelozerov.glowws.ui.screen.ai.AiScreen
import com.yaabelozerov.glowws.ui.screen.ai.AiScreenViewModel
import com.yaabelozerov.glowws.ui.screen.archive.ArchiveScreen
import com.yaabelozerov.glowws.ui.screen.archive.ArchiveScreenViewModel
import com.yaabelozerov.glowws.ui.screen.idea.IdeaScreen
import com.yaabelozerov.glowws.ui.screen.idea.IdeaScreenViewModel
import com.yaabelozerov.glowws.ui.screen.settings.FeedbackScreen
import com.yaabelozerov.glowws.ui.screen.settings.SettingsScreen
import com.yaabelozerov.glowws.ui.screen.settings.SettingsScreenViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun MainScreenNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: NavDestinations,
    mvm: MainScreenViewModel,
    ivm: IdeaScreenViewModel,
    svm: SettingsScreenViewModel,
    avm: ArchiveScreenViewModel,
    aivm: AiScreenViewModel,
    snackbar: Pair<SnackbarHostState, CoroutineScope>
) {
    NavHost(modifier = modifier,
        navController = navController,
        startDestination = startDestination.route,
        enterTransition = {
            fadeIn(
                tween(300)
            )
        },
        exitTransition = { fadeOut(tween(300)) }) {
        composable(NavDestinations.MainScreenRoute.route) {
            Column {
                MainScreen(
                    imageLoader = mvm.imageLoader,
                    ideas = mvm.state.collectAsState().value.ideas,
                    onClickIdea = { id ->
                        navController.navigate(NavDestinations.IdeaScreenRoute.withParam(id))
                        ivm.refreshPoints(id)
                    },
                    onArchiveIdea = { id -> mvm.archiveIdea(id) },
                    onSelect = { id -> mvm.onSelect(id) },
                    inSelectionMode = mvm.selection.collectAsState().value.inSelectionMode,
                    selection = mvm.selection.collectAsState().value.entries,
                    settings = svm.state.collectAsState().value
                )
            }
        }
        composable(route = NavDestinations.IdeaScreenRoute.withParam("{id}"),
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) + fadeIn()
            },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) + fadeOut() }) { backStackEntry ->
            val discardText = stringResource(R.string.m_idea_discarded)
            val aiStatus = aivm.aiStatus.collectAsState().value
            IdeaScreen(
                imageLoader = ivm.imageLoader,
                points = ivm.points.collectAsState().value,
                onBack = {
                    mvm.tryDiscardEmpty(backStackEntry.arguments!!.getLong("id")) {
                        snackbar.second.launch {
                            snackbar.first.showSnackbar(
                                discardText, duration = SnackbarDuration.Short
                            )
                        }
                    }
                    navController.navigateUp()
                },
                onAdd = { type, ind ->
                    ivm.addPointAtIndex(
                        type, backStackEntry.arguments!!.getLong("id"), ind
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
                    ivm.generateResponse(content, pointId, aivm.aiStatus.value.first?.token ?: "")
                },
                settings = svm.state.collectAsState().value,
                aiStatus = aivm.aiStatus.collectAsState().value
            )
        }
        composable(NavDestinations.SettingsScreenRoute.route) {
            SettingsScreen(settings = svm.state.collectAsState().value, onModify = { key, value ->
                svm.modifySetting(key, value) { mvm.fetchSort() }
            }, aiStatus = aivm.aiStatus.collectAsState().value, onNavigateToAi = {
                navController.navigate(NavDestinations.AiScreenRoute.route)
            }, onNavigateToFeedback = { navController.navigate(NavDestinations.FeedbackRoute.route) })
        }
        composable(
            NavDestinations.ArchiveScreenRoute.route
        ) {
            ArchiveScreen(
                imageLoader = avm.imageLoader,
                ideas = avm.state.collectAsState().value,
                onClick = { id ->
                    navController.navigate(NavDestinations.IdeaScreenRoute.withParam(id))
                    ivm.refreshPoints(id)
                },
                onRemove = { id -> avm.removeIdea(id) },
                onUnarchive = { id -> avm.unarchiveIdea(id) },
                onSelect = { id -> avm.onSelect(id) },
                selectionState = avm.selection.collectAsState().value,
                settings = svm.state.collectAsState().value
            )
        }
        composable(NavDestinations.AiScreenRoute.route,
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) + fadeIn()
            },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) + fadeOut() }) {
            AiScreen(models = aivm.models.collectAsState().value,
                onChoose = { aivm.pickModel(it) },
                onDelete = { aivm.removeModel(it) },
                onUnload = { aivm.unloadModel() },
                onAdd = { aivm.openLocalModelPicker() },
                onRefresh = { aivm.refresh() },
                onEdit = {
                    aivm.editModel(it)
                    aivm.refresh()
                },
                status = aivm.aiStatus.collectAsState().value,
                error = null
            )
        }
        composable(NavDestinations.FeedbackRoute.route, enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) + fadeIn()
        },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) + fadeOut() }) {
            FeedbackScreen {
                svm.sendFeedback(it)
                navController.navigateUp()
            }
        }
    }
}
