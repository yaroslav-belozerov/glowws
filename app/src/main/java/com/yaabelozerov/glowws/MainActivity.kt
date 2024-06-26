package com.yaabelozerov.glowws

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yaabelozerov.glowws.ui.screen.IdeaScreen
import com.yaabelozerov.glowws.ui.screen.MainScreen
import com.yaabelozerov.glowws.ui.theme.GlowwsTheme
import com.yaabelozerov.glowws.ui.viewmodel.MainScreenViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val vm: MainScreenViewModel by viewModels()
        vm.getIdeas()

        setContent {
            val navController = rememberNavController()

            GlowwsTheme {
                Scaffold(modifier = Modifier.fillMaxSize(), content = { innerPadding ->
                    NavHost(navController = navController, startDestination = "MainScreen") {
                        composable("MainScreen") {
                            MainScreen(
                                modifier = Modifier.padding(
                                    innerPadding
                                ), ideas = vm.state.collectAsState().value.ideas, onAdd = {  }, onRemove = { id -> vm.removeIdea(id) }
                            )
                        }
                        composable(
                            "IdeaScreen/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.LongType })
                        ) { backStackEntry ->
                            IdeaScreen(
                                modifier = Modifier.padding(
                                    innerPadding
                                ), points = emptyList(), onRemove = { }
                            )
                        }
                        composable("CreateIdea") {
                            val textFieldState = remember { mutableStateOf("") }
                            Column(
                                modifier = Modifier
                                    .padding(innerPadding)
                                    .padding(16.dp)
                                    .fillMaxSize()
                            ) {
                                TextField(
                                    value = textFieldState.value,
                                    onValueChange = { textFieldState.value = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row {
                                    OutlinedButton(onClick = {
                                        navController.popBackStack()
                                    }) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "Cancel")
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Button(onClick = {
                                        vm.addIdea(textFieldState.value)
                                        navController.popBackStack()
                                    }, modifier = Modifier.weight(1f)) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "Save")
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "save idea button"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }, floatingActionButton = {
                    FloatingActionButton(onClick = { navController.navigate("CreateIdea") }) {
                        Icon(
                            imageVector = Icons.Default.Add, contentDescription = "add idea button"
                        )
                    }
                })
            }
        }
    }
}