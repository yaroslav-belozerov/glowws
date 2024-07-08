package com.yaabelozerov.glowws

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yaabelozerov.glowws.ui.model.DialogEntry
import com.yaabelozerov.glowws.ui.screen.archive.ArchiveScreen
import com.yaabelozerov.glowws.ui.screen.archive.ArchiveScreenViewModel
import com.yaabelozerov.glowws.ui.screen.idea.IdeaScreen
import com.yaabelozerov.glowws.ui.screen.idea.IdeaScreenViewModel
import com.yaabelozerov.glowws.ui.screen.main.MainScreen
import com.yaabelozerov.glowws.ui.screen.main.ScreenSelectedDialog
import com.yaabelozerov.glowws.ui.theme.GlowwsTheme
import com.yaabelozerov.glowws.ui.screen.main.MainScreenViewModel
import com.yaabelozerov.glowws.ui.screen.main.TitleBar
import com.yaabelozerov.glowws.ui.screen.settings.SettingsScreen
import com.yaabelozerov.glowws.ui.screen.settings.SettingsScreenViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val mvm: MainScreenViewModel by viewModels()
        val ivm: IdeaScreenViewModel by viewModels()
        val svm: SettingsScreenViewModel by viewModels()
        val avm: ArchiveScreenViewModel by viewModels()

        setContent {
            val navController = rememberNavController()
            val inSelectionModeMain = remember {
                mutableStateOf(false)
            }
            val selectedIdeasMain = remember { mutableStateOf(emptyList<Long>()) }

            val inSelectionModeArchive = remember {
                mutableStateOf(false)
            }
            val selectedIdeasArchive = remember { mutableStateOf(emptyList<Long>()) }


            GlowwsTheme {
                Scaffold(modifier = Modifier.fillMaxSize(), content = { innerPadding ->
                    NavHost(navController = navController, startDestination = "MainScreen") {
                        composable("MainScreen") {
                            Column(Modifier.padding(innerPadding)) {
                                TitleBar(onSettings = { navController.navigate("SettingsScreen") },
                                    onArchive = { navController.navigate("ArchiveScreen") })
                                MainScreen(ideas = mvm.state.collectAsState().value.ideas,
                                    onSaveProject = { id, text -> mvm.modifyGroupName(id, text) },
                                    onArchiveProject = { id -> mvm.archiveGroup(id) },
                                    onClickIdea = { id ->
                                        navController.navigate("IdeaScreen/${id}")
                                        ivm.refreshPoints(id)
                                    },
                                    onAddIdeaToGroup = { groupId ->
                                        mvm.addIdeaToGroup("", groupId, callback = { id ->
                                            navController.navigate("IdeaScreen/${id}")
                                            ivm.refreshPoints(id)
                                        })
                                    },
                                    onArchiveIdea = { id -> mvm.archiveIdea(id) },
                                    inSelectionMode = inSelectionModeMain,
                                    selectedIdeas = selectedIdeasMain,
                                    settings = svm.state.collectAsState().value.map { it.value }
                                        .flatten())
                            }
                        }
                        composable(
                            "IdeaScreen/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.LongType })
                        ) { backStackEntry ->
                            IdeaScreen(modifier = Modifier.padding(
                                innerPadding
                            ), points = ivm.points.collectAsState().value, onBack = {
                                navController.navigateUp()
                            }, onAdd = {
                                ivm.addPoint(backStackEntry.arguments!!.getLong("id"))
                            }, onSave = { pointId, newText, isMain ->
                                ivm.modifyPoint(
                                    pointId, newText, isMain
                                )
                            }, onRemove = { pointId ->
                                ivm.removePoint(pointId)
                            })
                        }
                        composable(
                            "SettingsScreen"
                        ) {
                            SettingsScreen(modifier = Modifier.padding(innerPadding),
                                svm.state.collectAsState().value,
                                onModify = { key, value -> svm.modifySetting(key, value) })
                        }
                        composable(
                            "ArchiveScreen"
                        ) {
                            ArchiveScreen(
                                modifier = Modifier.padding(innerPadding),
                                ideas = avm.state.collectAsState().value,
                                onClick = { id ->
                                    navController.navigate("IdeaScreen/${id}")
                                    ivm.refreshPoints(id)
                                },
                                onRemove = { id -> avm.removeIdea(id) },
                                onUnarchive = { id -> avm.unarchiveIdea(id) },
                                inSelectionMode = inSelectionModeArchive,
                                selectedIdeas = selectedIdeasArchive,
                            )
                        }
                    }
                }, floatingActionButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (navController.currentBackStackEntryAsState().value?.destination?.route) {
                            "MainScreen" -> {
                                val isConfirmationOpen = remember {
                                    mutableStateOf(false)
                                }
                                if (isConfirmationOpen.value) {
                                    ScreenSelectedDialog(title = "Archive all selected?",
                                        entries = listOf(
                                            DialogEntry(
                                                Icons.Default.CheckCircle, "Confirm", {
                                                    selectedIdeasMain.value.forEach {
                                                        mvm.archiveIdea(
                                                            it
                                                        )
                                                    }
                                                    inSelectionModeMain.value = false
                                                }, needsConfirmation = false
                                            ), DialogEntry(
                                                null,
                                                "Cancel",
                                                onClick = { isConfirmationOpen.value = false },
                                                needsConfirmation = false
                                            )
                                        ),
                                        onDismiss = { isConfirmationOpen.value = false })
                                }
                                if (inSelectionModeMain.value) {
                                    FloatingActionButton(onClick = {
                                        isConfirmationOpen.value = true
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "archive selected button"
                                        )
                                    }
                                    FloatingActionButton(onClick = {
                                        inSelectionModeMain.value = false
                                        selectedIdeasMain.value = emptyList()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "deselect button"
                                        )
                                    }
                                } else FloatingActionButton(onClick = {
                                    mvm.addNewIdeaAndProject("", callback = { id ->
                                        navController.navigate("IdeaScreen/${id}")
                                        ivm.refreshPoints(id)
                                    })
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "add idea button"
                                    )
                                }
                            }

                            "ArchiveScreen" -> {
                                val isConfirmationOpen = remember {
                                    mutableStateOf(false)
                                }
                                if (isConfirmationOpen.value) {
                                    ScreenSelectedDialog(title = "Remove all selected?",
                                        entries = listOf(
                                            DialogEntry(
                                                Icons.Default.CheckCircle, "Confirm", {
                                                    selectedIdeasArchive.value.forEach {
                                                        avm.removeIdea(
                                                            it
                                                        )
                                                    }
                                                    inSelectionModeArchive.value = false
                                                }, needsConfirmation = false
                                            ), DialogEntry(
                                                null,
                                                "Cancel",
                                                onClick = { isConfirmationOpen.value = false },
                                                needsConfirmation = false
                                            )
                                        ),
                                        onDismiss = { isConfirmationOpen.value = false })
                                }
                                if (inSelectionModeArchive.value) {
                                    FloatingActionButton(onClick = {
                                        isConfirmationOpen.value = true
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "delete selected button"
                                        )
                                    }
                                    FloatingActionButton(onClick = {
                                        inSelectionModeArchive.value = false
                                        selectedIdeasArchive.value = emptyList()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "deselect button"
                                        )
                                    }
                                }
                            }
                        }
                    }
                })
            }
        }
    }
}