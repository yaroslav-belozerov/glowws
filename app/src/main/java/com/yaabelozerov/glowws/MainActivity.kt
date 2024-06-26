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
import androidx.room.Room
import com.yaabelozerov.glowws.data.local.room.Group
import com.yaabelozerov.glowws.data.local.room.Idea
import com.yaabelozerov.glowws.data.local.room.IdeaDatabase
import com.yaabelozerov.glowws.ui.screen.MainScreen
import com.yaabelozerov.glowws.ui.theme.GlowwsTheme
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

//        runBlocking {
//            val id = dao.createGroup(Group(0, "test_group2"))
//            dao.insert(Idea(0, id, "test_idea"))
//            dao.insert(Idea(0, id, "test_idea"))
//        }

        setContent {
            GlowwsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding), ideas = dao.getGroupsWithIdeas().collectAsState(
                            initial = emptyMap()
                        ).value
                    )
                }
            }
        }
    }
}