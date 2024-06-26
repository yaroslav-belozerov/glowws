package com.yaabelozerov.glowws

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import com.yaabelozerov.glowws.data.local.room.Group
import com.yaabelozerov.glowws.data.local.room.Idea
import com.yaabelozerov.glowws.data.local.room.IdeaDatabase
import com.yaabelozerov.glowws.data.local.room.Point
import com.yaabelozerov.glowws.domain.model.IdeaMapper
import com.yaabelozerov.glowws.domain.model.PointDomainModel
import com.yaabelozerov.glowws.ui.screen.IdeaScreen
import com.yaabelozerov.glowws.ui.screen.MainScreen
import com.yaabelozerov.glowws.ui.theme.GlowwsTheme
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = Room.databaseBuilder(
            applicationContext, IdeaDatabase::class.java, "glowws.db"
        ).build()
        val dao = db.ideaDao()
        val mapper = IdeaMapper()

        setContent {
            val navController = rememberNavController()

            GlowwsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(navController = navController, startDestination = "MainScreen") {
                        composable("MainScreen") {
                            MainScreen(modifier = Modifier.padding(
                                innerPadding
                            ), ideas = mapper.toDomainModel(
                                dao.getGroupsWithIdeas().collectAsState(
                                    initial = emptyMap()
                                ).value
                            ) { navController.navigate("IdeaScreen/$it") })
                        }
                        composable(
                            "IdeaScreen/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.LongType })
                        ) { backStackEntry ->
                            IdeaScreen(modifier = Modifier.padding(
                                innerPadding
                            ),
                                points = dao.getIdeaPoints(backStackEntry.arguments?.getLong("id")!!)
                                    .collectAsState(
                                        initial = emptyList()
                                    ).value.map { PointDomainModel(it.content, it.isMain) })
                        }
                    }
                }
            }
        }
    }
}