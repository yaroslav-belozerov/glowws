package com.yaabelozerov.glowws

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.ui.common.NavDestinations
import com.yaabelozerov.glowws.ui.common.toDestination
import com.yaabelozerov.glowws.ui.common.withParam
import com.yaabelozerov.glowws.ui.screen.ai.AiScreenViewModel
import com.yaabelozerov.glowws.ui.screen.archive.ArchiveScreenFloatingButtons
import com.yaabelozerov.glowws.ui.screen.archive.ArchiveScreenViewModel
import com.yaabelozerov.glowws.ui.screen.idea.IdeaScreenViewModel
import com.yaabelozerov.glowws.ui.screen.main.MainScreenFloatingButtons
import com.yaabelozerov.glowws.ui.screen.main.MainScreenNavHost
import com.yaabelozerov.glowws.ui.screen.main.MainScreenViewModel
import com.yaabelozerov.glowws.ui.screen.main.SortFilterModalBottomSheet
import com.yaabelozerov.glowws.ui.screen.main.booleanOrNull
import com.yaabelozerov.glowws.ui.screen.settings.SettingsScreenViewModel
import com.yaabelozerov.glowws.ui.theme.GlowwsTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val mvm: MainScreenViewModel by viewModels()
        val ivm: IdeaScreenViewModel by viewModels()
        val svm: SettingsScreenViewModel by viewModels()
        val avm: ArchiveScreenViewModel by viewModels()
        val aivm: AiScreenViewModel by viewModels()

        val onPickModel = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                aivm.viewModelScope.launch {
                    aivm.inferenceManager.importModel(uri) { aivm.setDefaultModel(uri) }
                    aivm.refresh()
                }
            }
        }
        aivm.onPickModel.value = { onPickModel.launch(arrayOf("*/*")) }

        val onPickMedia =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                uri?.let {
                    ivm.viewModelScope.launch {
                        ivm.importImage(uri)
                    }
                }
            }
        ivm.onPickMedia.value = {
            onPickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        setContent {
            val navController = rememberNavController()

            val dynamicColor =
                svm.state.collectAsState().value[SettingsKeys.MONET_THEME].booleanOrNull()
            if (dynamicColor != null) {
                GlowwsTheme(
                    dynamicColor = dynamicColor.toString().toBoolean()
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val dest = navBackStackEntry?.destination?.route?.toDestination()
                            dest?.let {
                                TopAppBar(title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        it.resId?.let { res ->
                                            Text(
                                                text = stringResource(id = res),
                                                fontSize = 32.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (it.route == NavDestinations.MainScreenRoute.route) {
                                            Icon(
                                                modifier = Modifier.size(16.dp),
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null
                                            )
                                        }
                                    }
                                })
                            }
                        },
                        content = { innerPadding ->
                            MainScreenNavHost(
                                modifier = Modifier.padding(innerPadding),
                                navController = navController,
                                startDestination = NavDestinations.MainScreenRoute,
                                mvm = mvm,
                                ivm = ivm,
                                svm = svm,
                                avm = avm,
                                aivm = aivm
                            )

                            SortFilterModalBottomSheet(mvm = mvm)
                        },
                        floatingActionButton = {
                            FloatingActionButtons(
                                navController,
                                mvm,
                                ivm,
                                avm
                            )
                        },
                        bottomBar = { BottomNavBar(navController) }
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(navController: NavHostController) {
    val items = listOf(
        NavDestinations.SettingsScreenRoute,
        NavDestinations.MainScreenRoute,
        NavDestinations.ArchiveScreenRoute,
    )
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        items.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(selected = selected, onClick = {
                navController.popBackStack(
                    screen.route,
                    inclusive = true,
                    saveState = true
                )
                navController.navigate(screen.route)
            }, icon = {
                Icon(
                    imageVector = if (selected) screen.iconActive!! else screen.iconInactive!!,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = if (!selected) Modifier.alpha(0.4f) else Modifier
                )
            })
        }
    }
}

@Composable
fun FloatingActionButtons(
    navController: NavHostController,
    mvm: MainScreenViewModel,
    ivm: IdeaScreenViewModel,
    avm: ArchiveScreenViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    when (navBackStackEntry?.destination?.route?.toDestination()) {
        NavDestinations.MainScreenRoute -> MainScreenFloatingButtons(
            mvm = mvm,
            addNewIdeaCallback = { id ->
                navController.navigate(
                    NavDestinations.IdeaScreenRoute.withParam(
                        id
                    )
                )
                ivm.refreshPoints(id)
            }
        )

        NavDestinations.ArchiveScreenRoute -> ArchiveScreenFloatingButtons(
            avm = avm
        )

        else -> {}
    }
}
