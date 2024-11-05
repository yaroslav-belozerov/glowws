package com.yaabelozerov.glowws.ui.screen.main

import android.util.Log
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
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
import com.yaabelozerov.glowws.ui.common.Nav
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
    innerPaddingValues: PaddingValues,
    navController: NavHostController,
    startDestination: Nav,
    mvm: MainScreenViewModel,
    ivm: IdeaScreenViewModel,
    svm: SettingsScreenViewModel,
    avm: ArchiveScreenViewModel,
    aivm: AiScreenViewModel,
    snackbar: Pair<SnackbarHostState, CoroutineScope>
) {
    NavHost(modifier = modifier,
        navController = navController,
        startDestination = startDestination.route, enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
        composable(Nav.MainScreenRoute.route) {
            MainScreen(
                modifier = Modifier,
                mvm = mvm,
                ideas = mvm.state.collectAsState().value.ideas,
                onClickIdea = { id ->
                    navController.navigate(Nav.IdeaScreenRoute.withParam(id))
                    ivm.refreshPoints(id)
                },
                onArchiveIdea = { id -> mvm.archiveIdea(id) },
                onSelect = { id -> mvm.onSelect(id) },
                onSetPriority = { id, priority -> mvm.setPriority(id, priority) },
                inSelectionMode = mvm.selection.collectAsState().value.inSelectionMode,
                selection = mvm.selection.collectAsState().value.entries,
                settings = svm.state.collectAsState().value,
                onNavgigateToFeedback = { navController.navigate(Nav.FeedbackRoute.route) },
                snackbar = snackbar
            )
        }
        composable(route = Nav.IdeaScreenRoute.withParam("{id}"),
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) + fadeIn()
            },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) + fadeOut() }) { backStackEntry ->
            val discardText = stringResource(R.string.m_idea_discarded)
            IdeaScreen(
                modifier = Modifier.consumeWindowInsets(innerPaddingValues),
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
                    ivm.generateResponse(content, pointId)
                },
                settings = svm.state.collectAsState().value,
                aiStatus = aivm.aiStatus.collectAsState().value,
            )
        }
        composable(Nav.SettingsScreenRoute.route) {
            SettingsScreen(settings = svm.state.collectAsState().value, onModify = { key, value ->
                svm.modifySetting(key, value) { mvm.fetchSort() }
            }, aiStatus = aivm.aiStatus.collectAsState().value, onNavigateToAi = {
                navController.navigate(Nav.AiScreenRoute.route)
            })
        }
        composable(
            Nav.ArchiveScreenRoute.route
        ) {
            ArchiveScreen(
                ideas = avm.state.collectAsState().value,
                onClick = { id ->
                    navController.navigate(Nav.IdeaScreenRoute.withParam(id))
                    ivm.refreshPoints(id)
                },
                onRemove = { id -> avm.removeIdea(id) },
                onUnarchive = { id -> avm.unarchiveIdea(id) },
                onSelect = { id -> avm.onSelect(id) },
                selectionState = avm.selection.collectAsState().value,
                settings = svm.state.collectAsState().value
            )
        }
        composable(Nav.AiScreenRoute.route,
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
                },
                status = aivm.aiStatus.collectAsState().value,
                error = null
            )
        }
        composable(Nav.FeedbackRoute.route, enterTransition = {
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
