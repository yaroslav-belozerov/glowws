package com.yaabelozerov.glowws

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.data.local.room.PointType
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
import android.Manifest
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.view.WindowCompat
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.yaabelozerov.glowws.data.remote.PreloadModelsService
import com.yaabelozerov.glowws.util.Network
import retrofit2.Retrofit
import retrofit2.awaitResponse
import retrofit2.converter.moshi.MoshiConverterFactory


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
                aivm.importLocalModel(uri)
            }
        }
        aivm.onPickModel.value = { onPickModel.launch(arrayOf("application/octet-stream")) }

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

            val snackbarHostState = remember {
                SnackbarHostState()
            }
            val scope = rememberCoroutineScope()
            val text = stringResource(R.string.app_welcome)
            LaunchedEffect(true) {
                mvm.appFirstVisit {
                    snackbarHostState.showSnackbar(
                        message = text, duration = SnackbarDuration.Short
                    )
                    try {
                        Retrofit.Builder().baseUrl(Network.MODEL_PRELOAD_BASE_URL).addConverterFactory(
                            MoshiConverterFactory.create(
                                Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                            )
                        ).build().create(PreloadModelsService::class.java).getModels().awaitResponse().also { Log.i("got", it.body().toString()) }
                            .body()?.let {
                                aivm.importRemoteModels(it)
                            }
                    } catch (_: Exception) {}
                }
            }
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
                                        SnackbarHost(snackbarHostState, snackbar = {
                                            Snackbar(
                                                snackbarData = it,
                                                shape = MaterialTheme.shapes.medium,
                                                modifier = Modifier
                                                    .height(80.dp)
                                                    .clickable {
                                                        it.dismiss()
                                                    },
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                contentColor = MaterialTheme.colorScheme.onBackground
                                            )
                                        })
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
                                aivm = aivm,
                                snackbar = Pair(snackbarHostState, scope)
                            )

                            SortFilterModalBottomSheet(mvm = mvm)
                        },
                        floatingActionButton = {
                            FloatingActionButtons(
                                navController, mvm, ivm, avm
                            )
                        },
                        bottomBar = { BottomNavBar(navController) },
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
                    screen.route, inclusive = true, saveState = true
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
        NavDestinations.MainScreenRoute -> MainScreenFloatingButtons(mvm = mvm,
            addNewIdeaCallback = { id ->
                navController.navigate(
                    NavDestinations.IdeaScreenRoute.withParam(
                        id
                    )
                )
                ivm.addPointAtIndex(PointType.TEXT, id, 0, "")
                ivm.refreshPoints(id)
            })

        NavDestinations.ArchiveScreenRoute -> ArchiveScreenFloatingButtons(
            avm = avm
        )

        else -> {}
    }
}
