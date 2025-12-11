package com.yaabelozerov.glowws.ui.screen.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.yaabelozerov.glowws.ui.screen.idea.IdeaScreen
import com.yaabelozerov.glowws.ui.screen.idea.IdeaScreenViewModel
import com.yaabelozerov.glowws.ui.screen.settings.FeedbackScreen
import com.yaabelozerov.glowws.ui.screen.settings.SettingsScreen
import com.yaabelozerov.glowws.ui.screen.settings.SettingsScreenViewModel
import kotlinx.coroutines.CoroutineScope

@Composable
fun App(
  innerPadding: PaddingValues,
  navCtrl: NavHostController,
  startDestination: Nav,
  mvm: MainScreenViewModel,
  ivm: IdeaScreenViewModel,
  svm: SettingsScreenViewModel,
  aivm: AiScreenViewModel,
  snackbar: Pair<SnackbarHostState, CoroutineScope>,
  onError: (Exception) -> Unit
) {
  LaunchedEffect(Unit) {
    mvm.fetchMainScreen()
    mvm.fetchSort()
  }
  NavHost(
    modifier = Modifier.padding(innerPadding),
    navController = navCtrl,
    startDestination = startDestination.route,
    enterTransition = { fadeIn() },
    exitTransition = { fadeOut() }) {
    composable(Nav.MainScreenRoute.route) {
      MainScreen(
        modifier = Modifier,
        mvm = mvm,
        ideas = mvm.state.collectAsState().value.ideas,
        onEvent = { event -> mvm.onEvent(event, navCtrl, ivm) },
        selection = mvm.select.collectAsState().value,
        settings = svm.state.collectAsState().value,
        snackbar = snackbar
      )
    }
    composable(
      route = Nav.IdeaScreenRoute.withParam("{id}"),
      arguments = listOf(navArgument("id") { type = NavType.LongType }),
      enterTransition = {
        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) + fadeIn()
      },
      exitTransition = {
        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) + fadeOut()
      }) { backStackEntry ->
      val discardText = stringResource(R.string.m_idea_discarded)
      IdeaScreen(
        modifier = Modifier.consumeWindowInsets(innerPadding),
        points = ivm.points.collectAsState().value,
        onEvent = {
          backStackEntry.arguments?.getLong("id")?.let { id ->
            ivm.onEvent(
              event = it, ideaId = id, onBack = {
//                            mvm.tryDiscardEmpty(id) {
//                              snackbar.second.launch {
//                                snackbar.first.showSnackbar(
//                                    discardText, duration = SnackbarDuration.Short)
//                              }
//                            }
                mvm.fetchMainScreen()
                navCtrl.navigateUp()
              }, onError = onError
            )
          } ?: onError(Exception("No idea id in backstack entry"))
        },
        settings = svm.state.collectAsState().value,
        aiStatus = aivm.aiStatus.collectAsState().value,
      )
    }
    composable(Nav.SettingsScreenRoute.route) {
      SettingsScreen(
        svm = svm,
        onModify = { key, value -> svm.modifySetting(key, value) { mvm.fetchSort() } },
        aiStatus = aivm.aiStatus.collectAsState().value,
        onNavigateToAi = { navCtrl.navigate(Nav.AiScreenRoute.route) })
    }
    composable(Nav.ArchiveScreenRoute.route) {
      ArchiveScreen(
        ideas = mvm.state.collectAsState().value.archivedIdeas, onEvent = { event ->
          mvm.onArchiveEvent(event, navCtrl, ivm)
        }, selectionState = mvm.archiveSelect.collectAsState().value, settings = svm.state.collectAsState().value
      )
    }
    composable(Nav.AiScreenRoute.route, enterTransition = {
      slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) + fadeIn()
    }, exitTransition = {
      slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) + fadeOut()
    }) {
      AiScreen(
        models = aivm.models.collectAsState().value,
        onEvent = { event -> aivm.onEvent(event) },
        status = aivm.aiStatus.collectAsState().value,
        error = null
      )
    }
    composable(Nav.FeedbackRoute.route, enterTransition = {
      slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) + fadeIn()
    }, exitTransition = {
      slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) + fadeOut()
    }) {
      FeedbackScreen { feedback ->
        svm.sendFeedback(feedback)
        navCtrl.navigateUp()
      }
    }
  }
}
