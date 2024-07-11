package com.yaabelozerov.glowws.ui.screen.main

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.domain.model.GroupDomainModel
import com.yaabelozerov.glowws.domain.model.IdeaDomainModel
import com.yaabelozerov.glowws.domain.model.SettingDomainModel
import com.yaabelozerov.glowws.ui.model.DialogEntry
import com.yaabelozerov.glowws.ui.theme.Typography

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    ideas: Map<GroupDomainModel, List<IdeaDomainModel>> = emptyMap(),
    onSaveProject: (Long, String) -> Unit,
    onArchiveProject: (Long) -> Unit,
    onClickIdea: (Long) -> Unit,
    onAddIdeaToGroup: (Long) -> Unit,
    onArchiveIdea: (Long) -> Unit,
    inSelectionMode: MutableState<Boolean>,
    selectedIdeas: MutableState<List<Long>>,
    settings: List<SettingDomainModel>
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
                Idea(
                    ideas[proj]!!.first().content,
                    ideas[proj]!!.first().modified,
                    { onClickIdea(ideas[proj]!!.first().id) },
                    { onAddIdeaToGroup(ideas[proj]!!.first().groupId) },
                    { onArchiveIdea(ideas[proj]!!.first().id) },
                    {
                        inSelectionMode.value = true
                        val id = ideas[proj]!!.first().id
                        if (selectedIdeas.value.contains(id)) {
                            selectedIdeas.value -= id
                            if (selectedIdeas.value.isEmpty()) inSelectionMode.value = false
                        } else {
                            selectedIdeas.value += id
                        }
                    },
                    inSelectionMode.value,
                    selectedIdeas.value.contains(ideas[proj]!!.first().id)
                )
            } else {
                Project(
                    name = proj.content,
                    modified = proj.modified,
                    ideas = ideas[proj]!!,
                    onSave = { newName -> onSaveProject(proj.id, newName) },
                    onArchive = { onArchiveProject(proj.id) },
                    onAddToGroup = { onAddIdeaToGroup(proj.id) },
                    onClickIdea = { id -> onClickIdea(id) },
                    onArchiveIdea = { id -> onArchiveIdea(id) },
                    onSelectIdea = { id ->
                        inSelectionMode.value = true
                        if (selectedIdeas.value.contains(id)) {
                            selectedIdeas.value -= id
                            if (selectedIdeas.value.isEmpty()) inSelectionMode.value = false
                        } else {
                            selectedIdeas.value += id
                        }
                    },
                    inSelectionMode.value,
                    selectedIdeas.value,
                    displayPlaceholder = settings.findLast { it.key == SettingsKeys.SHOW_PROJECT_EMPTY_NAME }?.value.toString() == "true"
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Idea(
    previewText: String,
    modified: String,
    onClick: () -> Unit,
    onAddToGroup: () -> Unit,
    onArchive: () -> Unit,
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
                    2.dp, MaterialTheme.colorScheme.primary
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
            text = previewText.ifBlank { "Empty" },
            Modifier
                .padding(16.dp)
                .weight(1f),
            style = Typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = if (previewText.isBlank()) 0.3f else 1f)
        )
        Spacer(modifier = Modifier.width(16.dp))
    }

    if (isDialogOpen.value) {
        ScreenSelectedDialog(title = previewText, info = listOf(modified), entries = listOf(
            DialogEntry(Icons.Default.Menu, "Select", {
                onSelect()
            }), DialogEntry(
                Icons.Default.AddCircle, "Add to Project", onAddToGroup
            ), DialogEntry(
                Icons.Default.Delete, "Archive Idea", onArchive, needsConfirmation = true
            )
        ), onDismiss = { isDialogOpen.value = false })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NestedIdea(
    previewText: String,
    modified: String,
    onClick: () -> Unit,
    onArchive: () -> Unit,
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
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(onClick = if (!inSelectionMode) {
                onClick
            } else {
                onSelect
            }, onLongClick = { if (!inSelectionMode) isDialogOpen.value = true })
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = previewText.ifBlank { "Empty" },
                modifier = Modifier.padding(8.dp),
                style = Typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (previewText.isBlank()) 0.3f else 1f)
                )
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
    }
    if (isDialogOpen.value) {
        ScreenSelectedDialog(title = previewText, info = listOf(modified), entries = listOf(
            DialogEntry(Icons.Default.Menu, "Select", {
                onSelect()
            }), DialogEntry(
                Icons.Default.Delete, "Archive Idea", onArchive, needsConfirmation = true
            )
        ), onDismiss = { isDialogOpen.value = false })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Project(
    name: String,
    modified: String,
    ideas: List<IdeaDomainModel>,
    onSave: (String) -> Unit,
    onArchive: () -> Unit,
    onAddToGroup: () -> Unit,
    onClickIdea: (Long) -> Unit,
    onArchiveIdea: (Long) -> Unit,
    onSelectIdea: (Long) -> Unit,
    inSelection: Boolean,
    currentSelection: List<Long>,
    displayPlaceholder: Boolean
) {
    val isBeingModified = remember {
        mutableStateOf(
            false
        )
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
            if (name.isNotBlank()) {
                Text(
                    text = name.ifBlank { "Unnamed Project" },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (name.isBlank()) 0.3f else 1f)
                )
            } else if (displayPlaceholder) {
                Text(
                    text = "Unnamed Project",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            }
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
                NestedIdea(
                    previewText = it.content,
                    modified = it.modified,
                    onClick = { onClickIdea(it.id) },
                    onArchive = { onArchiveIdea(it.id) },
                    onSelect = { onSelectIdea(it.id) },
                    inSelectionMode = inSelection,
                    isSelected = currentSelection.contains(it.id)
                )
            }
        }
    }
    if (isDialogOpen.value) {
        ScreenSelectedDialog(title = name, info = listOf(modified), entries = listOf(
            DialogEntry(Icons.Default.Menu, "Select", {
                ideas.forEach { onSelectIdea(it.id) }
            }),
            DialogEntry(Icons.Default.AddCircle, "Add to Project", {
                onAddToGroup()
            }),
            DialogEntry(Icons.Default.Edit, "Edit Project Name", {
                isBeingModified.value = true
            }),
            DialogEntry(
                Icons.Default.Delete, "Archive Project", onArchive, needsConfirmation = true
            ),
        ), onDismiss = { isDialogOpen.value = false })
    }
}

@Composable
fun TitleBar(modifier: Modifier = Modifier, onSettings: () -> Unit, onArchive: () -> Unit) {
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
                .clickable { onArchive() }
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
                    onSettings()
                }
                .size(32.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    }
}

@Composable
fun ScreenSelectedDialog(
    title: String,
    info: List<String> = emptyList(),
    entries: List<DialogEntry>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        val confirm = remember {
            mutableStateOf(List(entries.size) { false })
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title, fontSize = 32.sp, textAlign = TextAlign.Center
            )
            info.forEach { Text(text = it) }
            entries.forEachIndexed { ind, it ->
                if (!it.needsConfirmation) {
                    DialogButton(
                        icon = it.icon, text = it.name, onClick = {
                            it.onClick()
                            onDismiss()
                        }, isActive = false
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
                        {
                            it.onClick()
                            onDismiss()
                        }
                    } else {
                        { confirm.value = List(entries.size) { it == ind } }
                    }, isActive = confirm.value[ind])
                }
            }
        }
    }
}

@Composable
fun DialogButton(icon: ImageVector?, text: String, onClick: () -> Unit, isActive: Boolean) {
    if (isActive) Button(onClick = onClick, content = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(4.dp)
        ) {
            Text(
                text = text, fontSize = 16.sp
            )
            if (icon != null) {
                Icon(
                    imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp)
                )
            }
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
            if (icon != null) {
                Icon(
                    imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp)
                )
            }
        }
    })
}