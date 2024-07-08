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
import com.yaabelozerov.glowws.data.local.datastore.SettingsDefaults
import com.yaabelozerov.glowws.ui.model.DialogEntry
import com.yaabelozerov.glowws.ui.screen.idea.IdeaScreen
import com.yaabelozerov.glowws.ui.screen.idea.IdeaScreenViewModel
import com.yaabelozerov.glowws.ui.screen.main.MainScreen
import com.yaabelozerov.glowws.ui.screen.main.MainScreenDialog
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
        svm.getSettings()
        mvm.getIdeas()

        setContent {
            val navController = rememberNavController()
            val inSelectionMode = remember {
                mutableStateOf(false)
            }
            val selectedIdeas = remember { mutableStateOf(emptyList<Long>()) }


            GlowwsTheme {
                Scaffold(modifier = Modifier.fillMaxSize(), content = { innerPadding ->
                    NavHost(navController = navController, startDestination = "MainScreen") {
                        composable("MainScreen") {
                            Column(Modifier.padding(innerPadding)) {
                                TitleBar(onSettings = { navController.navigate("SettingsScreen") })
                                MainScreen(ideas = mvm.state.collectAsState().value.ideas,
                                    onSaveProject = { id, text -> mvm.modifyGroupName(id, text) },
                                    onRemoveProject = { id -> mvm.removeGroup(id) },
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
                                    onRemoveIdea = { id -> mvm.removeIdea(id) },
                                    inSelectionMode = inSelectionMode,
                                    selectedIdeas = selectedIdeas,
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
                                navController.navigate("MainScreen")
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
                            SettingsScreen(
                                modifier = Modifier.padding(innerPadding),
                                svm.state.collectAsState().value,
                                onModify = { key, value -> svm.modifySetting(key, value) }
                            )
                        }
                    }
                }, floatingActionButton = {
                    val isConfirmationOpen = remember {
                        mutableStateOf(false)
                    }
                    if (isConfirmationOpen.value) {
                        MainScreenDialog(title = "Remove all selected?", entries = listOf(
                            DialogEntry(
                                Icons.Default.CheckCircle, "Confirm", {
                                    selectedIdeas.value.forEach { mvm.removeIdea(it) }
                                    inSelectionMode.value = false
                                }, needsConfirmation = false
                            ), DialogEntry(
                                null,
                                "Cancel",
                                onClick = { isConfirmationOpen.value = false },
                                needsConfirmation = false
                            )
                        ), onDismiss = { isConfirmationOpen.value = false })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (inSelectionMode.value) {
                            FloatingActionButton(onClick = { isConfirmationOpen.value = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "delete selected button"
                                )
                            }
                            FloatingActionButton(onClick = {
                                inSelectionMode.value = false
                                selectedIdeas.value = emptyList()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "deselect button"
                                )
                            }
                        }
                        if (!inSelectionMode.value && navController.currentBackStackEntryAsState().value?.destination?.route == "MainScreen") {
                            FloatingActionButton(onClick = {
                                mvm.addNewIdea("", callback = { id ->
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
                    }
                })
            }
        }
    }
}