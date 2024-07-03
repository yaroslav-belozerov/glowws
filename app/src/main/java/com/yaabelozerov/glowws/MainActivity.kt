package com.yaabelozerov.glowws

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yaabelozerov.glowws.ui.screen.idea.IdeaScreen
import com.yaabelozerov.glowws.ui.screen.idea.IdeaScreenViewModel
import com.yaabelozerov.glowws.ui.screen.main.MainScreen
import com.yaabelozerov.glowws.ui.theme.GlowwsTheme
import com.yaabelozerov.glowws.ui.screen.main.MainScreenViewModel
import com.yaabelozerov.glowws.ui.screen.main.TitleBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val mvm: MainScreenViewModel by viewModels()
        val ivm: IdeaScreenViewModel by viewModels()
        mvm.getIdeas()

        setContent {
            val navController = rememberNavController()

            GlowwsTheme {
                Scaffold(modifier = Modifier.fillMaxSize(), content = { innerPadding ->
                    NavHost(navController = navController, startDestination = "MainScreen") {
                        composable("MainScreen") {
                            Column(Modifier.padding(innerPadding)) {
                                TitleBar()
                                MainScreen(ideas = mvm.state.collectAsState().value.ideas,
                                    onSaveProject = { id, text -> mvm.modifyGroupName(id, text) },
                                    onRemoveProject = { id -> mvm.removeGroup(id) },
                                    onClickIdea = { id ->
                                        navController.navigate("IdeaScreen/${id}")
                                        ivm.refreshPoints(id)
                                    },
                                    onAddIdeaToGroup = { groupId ->
                                        mvm.addIdeaToGroup("", groupId)
                                    },
                                    onRemoveIdea = { id -> mvm.removeIdea(id) }
                                )
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
                    }
                }, floatingActionButton = {
                    if (navController.currentBackStackEntryAsState().value?.destination?.route == "MainScreen") {
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
                })
            }
        }
    }
}