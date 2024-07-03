package com.yaabelozerov.glowws.ui.screen.main

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.yaabelozerov.glowws.domain.model.GroupDomainModel
import com.yaabelozerov.glowws.domain.model.IdeaDomainModel
import com.yaabelozerov.glowws.ui.model.DialogEntry
import com.yaabelozerov.glowws.ui.theme.Typography

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    ideas: Map<GroupDomainModel, List<IdeaDomainModel>> = emptyMap(),
    onSaveProject: (Long, String) -> Unit,
    onRemoveProject: (Long) -> Unit,
    onClickidea: (Long) -> Unit,
    onAddIdeaToGroup: (Long) -> Unit,
    onRemoveIdea: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(ideas.keys.toList()) { proj ->
            if (ideas[proj]!!.size == 1) {
                Idea(ideas[proj]!!.first().content,
                    { onClickidea(ideas[proj]!!.first().id) },
                    { onAddIdeaToGroup(ideas[proj]!!.first().groupId) },
                    { onRemoveIdea(ideas[proj]!!.first().id) })
            } else {
                Project(
                    name = proj.name,
                    ideas = ideas[proj]!!,
                    onSave = { newName -> onSaveProject(proj.id, newName) },
                    onRemove = { onRemoveProject(proj.id) },
                    onClickIdea = { id -> onClickidea(id) },
                    onRemoveIdea = { id -> onRemoveIdea(id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Idea(
    previewText: String, onClick: () -> Unit, onAddToGroup: () -> Unit, onRemove: () -> Unit
) {
    val isDialogDisplayed = remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .combinedClickable(onClick = onClick, onLongClick = { isDialogDisplayed.value = true })
    ) {
        Text(
            text = previewText,
            Modifier
                .padding(16.dp)
                .weight(1f),
            style = Typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.width(16.dp))
        Icon(imageVector = Icons.Default.Add,
            contentDescription = "add to group icon",
            modifier = Modifier
                .clickable { onAddToGroup() }
                .size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Icon(imageVector = Icons.Default.Delete,
            contentDescription = "delete idea icon",
            modifier = Modifier
                .clickable { onRemove() }
                .size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
    }

    if (isDialogDisplayed.value) {
        Dialog(onDismissRequest = { isDialogDisplayed.value = false }) {
            val entries = listOf(DialogEntry(
                Icons.Default.AddCircle, "Add to Project"
            ) { onAddToGroup() }, DialogEntry(
                Icons.Default.Delete, "Remove"
            ) { onRemove() })
            LazyColumn {
                items(entries) {
                    Button(onClick = it.onClick) {
                        Text(text = it.name)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NestedIdea(
    previewText: String, onClick: () -> Unit, onRemove: () -> Unit
) {
    val isDialogDisplayed = remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = { isDialogDisplayed.value = true })
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = previewText, modifier = Modifier.padding(8.dp), style = Typography.bodyLarge
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(imageVector = Icons.Default.Delete,
                contentDescription = "delete idea icon",
                modifier = Modifier
                    .clickable { onRemove() }
                    .size(16.dp))
            Spacer(modifier = Modifier.width(16.dp))
        }
    }
    if (isDialogDisplayed.value) {
        Dialog(onDismissRequest = { isDialogDisplayed.value = false }) {
            val entries = listOf(
                DialogEntry(Icons.Default.Delete, "Remove") { onRemove() }
            )
            LazyColumn {
                items(entries) {
                    Button(onClick = it.onClick) {
                        Text(text = it.name)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Project(
    name: String,
    ideas: List<IdeaDomainModel>,
    onSave: (String) -> Unit,
    onRemove: () -> Unit,
    onClickIdea: (Long) -> Unit,
    onRemoveIdea: (Long) -> Unit
) {
    val isBeingModified = remember {
        mutableStateOf(name.isBlank())
    }
    val isDialogOpen = remember {
        mutableStateOf(false)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .combinedClickable(onClick = {}, onLongClick = { isDialogOpen.value = true })
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isBeingModified.value) {
            Text(text = name,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { isBeingModified.value = true })
        } else {
            val txt = remember {
                mutableStateOf(name)
            }
            TextField(value = txt.value, onValueChange = {
                txt.value = it
            })
            Button(onClick = {
                onSave(txt.value)
                isBeingModified.value = false
            }) {
                Text(text = "Save")
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ideas.forEach {
                NestedIdea(previewText = it.content,
                    onClick = { onClickIdea(it.id) },
                    onRemove = { onRemoveIdea(it.id) })
            }
        }
    }
    if (isDialogOpen.value) {
        Dialog(onDismissRequest = { isDialogOpen.value = false }) {
            val entries = listOf(
                DialogEntry(Icons.Default.Delete, "Remove project", { onRemove() })
            )
            LazyColumn {
                items(entries) {
                    Button(onClick = it.onClick) {
                        Text(text = it.name)
                    }
                }
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
    MainScreen(modifier = Modifier, ideas = emptyMap(), { a, b -> }, {}, {}, {}, {})
}

@Preview
@Composable
fun IdeaPreview() {
    Idea(previewText = "Idea preview", {}, {}, {})
}

@Preview
@Composable
fun ProjectPreview() {
    Project("Project!",
        listOf(IdeaDomainModel(0, 0, "Project idea preview!")),
        {},
        {},
        {},
        {},
    )
}