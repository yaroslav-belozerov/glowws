package com.yaabelozerov.glowws.ui.screen.main

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    onClickIdea: (Long) -> Unit,
    onAddIdeaToGroup: (Long) -> Unit,
    onRemoveIdea: (Long) -> Unit,
    inSelectionMode: MutableState<Boolean>,
    selectedIdeas: MutableState<List<Long>>
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
                    { onClickIdea(ideas[proj]!!.first().id) },
                    { onAddIdeaToGroup(ideas[proj]!!.first().groupId) },
                    { onRemoveIdea(ideas[proj]!!.first().id) },
                    {
                        inSelectionMode.value = true
                        val id = ideas[proj]!!.first().id
                        if (selectedIdeas.value.contains(id)) {
                            selectedIdeas.value -= id
                        } else {
                            selectedIdeas.value += id
                        }
                    },
                    inSelectionMode.value,
                    selectedIdeas.value.contains(ideas[proj]!!.first().id)
                )
            } else {
                Project(
                    name = proj.name,
                    ideas = ideas[proj]!!,
                    onSave = { newName -> onSaveProject(proj.id, newName) },
                    onRemove = { onRemoveProject(proj.id) },
                    onClickIdea = { id -> onClickIdea(id) },
                    onRemoveIdea = { id -> onRemoveIdea(id) },
                    onSelectIdea = { id ->
                        inSelectionMode.value = true
                        if (selectedIdeas.value.contains(id)) {
                            selectedIdeas.value -= id
                        } else {
                            selectedIdeas.value += id
                        }
                    },
                    inSelectionMode.value,
                    selectedIdeas.value
                )
            }
        }
    }
}

data class MainScreenSelection(
    val isSelectionMode: Boolean, val selectedIdeaIds: MutableList<Long>
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Idea(
    previewText: String,
    onClick: () -> Unit,
    onAddToGroup: () -> Unit,
    onRemove: () -> Unit,
    onSelect: () -> Unit,
    inSelectionMode: Boolean,
    isSelected: Boolean
) {
    val isDialogOpen = remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically, modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.border(
                    2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium
                ) else Modifier
            )
            .background(MaterialTheme.colorScheme.primaryContainer)
            .combinedClickable(onClick = if (!inSelectionMode) {
                onClick
            } else {
                onSelect
            }, onLongClick = { if (!inSelectionMode) isDialogOpen.value = true })
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
    }

    if (isDialogOpen.value) {
        MainScreenDialog(title = previewText, entries = listOf(
            DialogEntry(Icons.Default.Menu, "Select", {
                onSelect()
                isDialogOpen.value = false
            }), DialogEntry(
                Icons.Default.AddCircle, "Add to Project", onAddToGroup
            ), DialogEntry(
                Icons.Default.Delete, "Remove Idea", onRemove, needsConfirmation = true
            )
        ), onDismiss = { isDialogOpen.value = false })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NestedIdea(
    previewText: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onSelect: () -> Unit,
    inSelectionMode: Boolean,
    isSelected: Boolean
) {
    val isDialogOpen = remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background
        ), modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.border(
                    2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium
                ) else Modifier
            )
            .combinedClickable(onClick = if (!inSelectionMode) {
                onClick
            } else {
                onSelect
            }, onLongClick = { if (!inSelectionMode) isDialogOpen.value = true })
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = previewText, modifier = Modifier.padding(8.dp), style = Typography.bodyLarge
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
    }
    if (isDialogOpen.value) {
        MainScreenDialog(title = previewText, entries = listOf(
            DialogEntry(Icons.Default.Menu, "Select", {
                onSelect()
                isDialogOpen.value = false
            }), DialogEntry(
                Icons.Default.Delete, "Remove Idea", onRemove, needsConfirmation = true
            )
        ), onDismiss = { isDialogOpen.value = false })
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
    onRemoveIdea: (Long) -> Unit,
    onSelectIdea: (Long) -> Unit,
    inSelection: Boolean,
    currentSelection: List<Long>
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
            .combinedClickable(onClick = { if (inSelection) ideas.forEach { onSelectIdea(it.id) } },
                onLongClick = { if (!inSelection) isDialogOpen.value = true })
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isBeingModified.value) {
            Text(
                text = name,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        } else {
            val txt = remember {
                mutableStateOf(name)
            }
            OutlinedTextField(value = txt.value, onValueChange = {
                txt.value = it
            })
            Button(onClick = {
                onSave(txt.value)
                isBeingModified.value = false
            }, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Save")
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ideas.forEach {
                NestedIdea(previewText = it.content,
                    onClick = { onClickIdea(it.id) },
                    onRemove = { onRemoveIdea(it.id) },
                    onSelect = { onSelectIdea(it.id) },
                    inSelectionMode = inSelection,
                    isSelected = currentSelection.contains(it.id)
                )
            }
        }
    }
    if (isDialogOpen.value) {
        MainScreenDialog(title = name, entries = listOf(
            DialogEntry(
                Icons.Default.Delete, "Remove Project", onRemove, needsConfirmation = true
            ), DialogEntry(Icons.Default.Edit, "Edit Project Name", {
                isBeingModified.value = true
                isDialogOpen.value = false
            })
        ), onDismiss = { isDialogOpen.value = false })
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

@Composable
fun MainScreenDialog(title: String, entries: List<DialogEntry>, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        val confirm = remember {
            mutableStateOf(List(entries.size) { false })
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title, fontSize = 32.sp
            )
            entries.forEachIndexed { ind, it ->
                if (!it.needsConfirmation) {
                    DialogButton(
                        icon = it.icon, text = it.name, onClick = it.onClick, isActive = false
                    )
                } else {
                    DialogButton(icon = if (confirm.value[ind]) {
                        Icons.Default.CheckCircle
                    } else {
                        it.icon
                    }, text = if (confirm.value[ind]) {
                        "Are you sure?"
                    } else {
                        it.name
                    }, onClick = if (confirm.value[ind]) {
                        it.onClick
                    } else {
                        { confirm.value = List(entries.size) { it == ind } }
                    }, isActive = confirm.value[ind])
                }
            }
        }
    }
}

@Composable
fun DialogButton(icon: ImageVector, text: String, onClick: () -> Unit, isActive: Boolean) {
    if (isActive) Button(onClick = onClick, content = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(4.dp)
        ) {
            Text(
                text = text, fontSize = 16.sp
            )
            Icon(
                imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp)
            )
        }
    }) else OutlinedButton(onClick = onClick, content = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(4.dp)
        ) {
            Text(
                text = text, fontSize = 16.sp
            )
            Icon(
                imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp)
            )
        }
    })
}