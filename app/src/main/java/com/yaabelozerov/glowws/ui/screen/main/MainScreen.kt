package com.yaabelozerov.glowws.ui.screen.main

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.yaabelozerov.glowws.domain.model.GroupDomainModel
import com.yaabelozerov.glowws.domain.model.IdeaDomainModel
import com.yaabelozerov.glowws.ui.theme.Typography

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    ideas: Map<GroupDomainModel, List<IdeaDomainModel>> = emptyMap(),
    onClick: (Long) -> Unit,
    onRemove: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(ideas.keys.toList()) { id ->
            if (ideas[id]!!.size == 1) {
                Idea(
                    ideas[id]!!.first().content,
                    { onClick(ideas[id]!!.first().id) },
                    { onRemove(ideas[id]!!.first().id) },
                )
            } else {
                Project(name = id.name, ideas = ideas[id]!!, onRemove = { id -> onRemove(id) })
            }
        }
    }
}

@Composable
fun Idea(previewText: String, onClick: () -> Unit, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable { onClick() }) {
        Text(
            text = previewText,
            Modifier
                .padding(16.dp)
                .weight(1f),
            style = Typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.width(16.dp))
        Icon(imageVector = Icons.Default.Delete,
            contentDescription = "delete idea icon",
            modifier = Modifier
                .clickable { onRemove() }
                .size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
    }
}

@Composable
fun NestedIdea(previewText: String, onClick: () -> Unit, onRemove: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = previewText, modifier = Modifier.padding(8.dp), style = Typography.bodyLarge
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(imageVector = Icons.Default.Delete,
                contentDescription = "delete idea icon",
                modifier = Modifier
                    .clickable { onRemove() }
                    .size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
        }
    }
}

@Composable
fun Project(name: String, ideas: List<IdeaDomainModel>, onRemove: (Long) -> Unit) {
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
            ideas.forEach {
                NestedIdea(previewText = it.content, onClick = {}, onRemove = { onRemove(it.id) })
            }
        }
    }
}

@Preview
@Composable
fun TitleBar(modifier: Modifier = Modifier) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp, 0.dp, 0.dp, 16.dp),
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

@Preview
@Composable
fun MainScreenPreview() {
    MainScreen(modifier = Modifier, ideas = emptyMap(), {}, {})
}

@Preview
@Composable
fun IdeaPreview() {
    Idea(previewText = "Idea preview", {}, {})
}

@Preview
@Composable
fun ProjectPreview() {
    Project("Project!", listOf(IdeaDomainModel(0, "Project idea preview!")), {})
}