package com.yaabelozerov.glowws.ui.screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaabelozerov.glowws.data.local.room.Group
import com.yaabelozerov.glowws.data.local.room.Idea
import com.yaabelozerov.glowws.ui.theme.Typography

@Composable
fun MainScreen(modifier: Modifier, ideas: Map<Group, List<Idea>> = emptyMap()) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(modifier)
                    .padding(0.dp, 16.dp, 0.dp, 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Icon(imageVector = Icons.Default.Delete,
                    contentDescription = "archive button",
                    modifier = Modifier
                        .clickable { Log.i("MainScreen", "Archive button clicked") }
                        .size(32.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                Text(
                    text = "Glowws",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(imageVector = Icons.Default.Settings,
                    contentDescription = "settings button",
                    modifier = Modifier
                        .clickable {
                            Log.i(
                                "MainScreen", "Settings button clicked"
                            )
                        }
                        .size(32.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            }
        }
        items(ideas.keys.toList()) { id ->
            if (ideas[id]!!.size == 1) {
                Idea(ideas[id]!!.first().content)
            } else {
                Project(name = id.name, ideaPreviews = ideas[id]!!.map { it.content })
            }
        }
    }
}

@Composable
fun Idea(previewText: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        Text(
            text = previewText,
            Modifier.padding(16.dp),
            style = Typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun NestedIdea(previewText: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = previewText, modifier = Modifier.padding(8.dp), style = Typography.bodyLarge
        )
    }
}

@Composable
fun Project(name: String, ideaPreviews: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = name, fontSize = 24.sp, fontWeight = FontWeight.SemiBold
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ideaPreviews.forEach {
                NestedIdea(previewText = it)
            }
        }
    }
}

@Preview
@Composable
fun MainScreenPreview() {
    MainScreen(modifier = Modifier, ideas = emptyMap())
}

@Preview
@Composable
fun IdeaPreview() {
    Idea(previewText = "Idea preview")
}

@Preview
@Composable
fun ProjectPreview() {
    Project("Project!", listOf("Project idea preview!"))
}