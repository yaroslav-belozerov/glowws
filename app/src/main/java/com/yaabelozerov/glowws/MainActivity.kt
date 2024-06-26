package com.yaabelozerov.glowws

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.room.Room
import androidx.room.RoomDatabase
import com.yaabelozerov.glowws.data.local.room.IdeaDatabase
import com.yaabelozerov.glowws.data.local.room.IdeaEntity
import com.yaabelozerov.glowws.ui.screen.MainScreen
import com.yaabelozerov.glowws.ui.theme.GlowwsTheme
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = Room.databaseBuilder(
            applicationContext, IdeaDatabase::class.java, "idea.db"
        ).build()
        val dao = db.ideaDao()

        runBlocking {
            dao.insert(IdeaEntity(0, "First idea"))
            dao.insert(IdeaEntity(0, "Second idea"))
            dao.insert(IdeaEntity(0, "Third idea"))
        }

        setContent {
            GlowwsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding),
                        ideas = dao.getAll().collectAsState(
                            initial = emptyList()
                        ).value.also { Log.i("MainActivity", "ideas: $it") })
                }
            }
        }
    }
}