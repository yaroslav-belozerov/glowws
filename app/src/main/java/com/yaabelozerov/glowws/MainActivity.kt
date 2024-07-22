package com.yaabelozerov.glowws

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yaabelozerov.glowws.ui.common.NavDestinations
import com.yaabelozerov.glowws.ui.common.withParam
import com.yaabelozerov.glowws.ui.screen.archive.ArchiveScreenFloatingButtons
import com.yaabelozerov.glowws.ui.screen.archive.ArchiveScreenViewModel
import com.yaabelozerov.glowws.ui.screen.idea.IdeaScreenViewModel
import com.yaabelozerov.glowws.ui.screen.main.MainScreenFloatingButtons
import com.yaabelozerov.glowws.ui.screen.main.MainScreenNavHost
import com.yaabelozerov.glowws.ui.screen.main.MainScreenViewModel
import com.yaabelozerov.glowws.ui.screen.main.SortFilterModalBottomSheet
import com.yaabelozerov.glowws.ui.screen.settings.SettingsScreenViewModel
import com.yaabelozerov.glowws.ui.theme.GlowwsTheme
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

            GlowwsTheme {
                Scaffold(modifier = Modifier.fillMaxSize(), content = { innerPadding ->
                    MainScreenNavHost(
                        modifier = Modifier.padding(innerPadding),
                        navController = navController,
                        startDestination = NavDestinations.MainScreenRoute,
                        mvm = mvm,
                        ivm = ivm,
                        svm = svm,
                        avm = avm,
                    )

                    SortFilterModalBottomSheet(mvm = mvm)
                }, floatingActionButton = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        when (navController.currentBackStackEntryAsState().value?.destination?.route) {
                            NavDestinations.MainScreenRoute.route -> MainScreenFloatingButtons(
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

                            NavDestinations.ArchiveScreenRoute.route -> ArchiveScreenFloatingButtons(
                                avm = avm
                            )
                        }
                    }
                })
            }
        }
    }
}
