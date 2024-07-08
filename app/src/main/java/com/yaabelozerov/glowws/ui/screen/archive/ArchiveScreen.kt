package com.yaabelozerov.glowws.ui.screen.archive

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaabelozerov.glowws.domain.model.IdeaDomainModel
import com.yaabelozerov.glowws.ui.model.DialogEntry
import com.yaabelozerov.glowws.ui.screen.main.ScreenSelectedDialog
import com.yaabelozerov.glowws.ui.theme.Typography

@Composable
fun ArchiveScreen(
    modifier: Modifier = Modifier,
    ideas: List<IdeaDomainModel> = emptyList(),
    onClick: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onUnarchive: (Long) -> Unit,
    inSelectionMode: MutableState<Boolean>,
    selectedIdeas: MutableState<List<Long>>,
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(text = "Archive", fontSize = 32.sp, modifier = Modifier.padding(16.dp, 8.dp).fillParentMaxWidth(), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        items(ideas) {
            ArchiveIdea(previewText = it.content,
                onClick = { onClick(it.id) },
                onRemove = { onRemove(it.id) },
                onUnarchive = { onUnarchive(it.id) },
                onSelect = {
                    inSelectionMode.value = true
                    if (selectedIdeas.value.contains(it.id)) {
                        selectedIdeas.value -= it.id
                        if (selectedIdeas.value.isEmpty()) inSelectionMode.value = false
                    } else {
                        selectedIdeas.value += it.id
                    }
                },
                inSelectionMode = inSelectionMode.value,
                isSelected = selectedIdeas.value.contains(it.id)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArchiveIdea(
    previewText: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onUnarchive: () -> Unit,
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
            .combinedClickable(onClick = if (inSelectionMode) onSelect else onClick,
                onLongClick = { if (!inSelectionMode) isDialogOpen.value = true })
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
        ScreenSelectedDialog(title = previewText, entries = listOf(
            DialogEntry(Icons.Default.Menu, "Select", {
                onSelect()
            }), DialogEntry(
                Icons.Default.Refresh, "Unarchive", onUnarchive
            ), DialogEntry(
                Icons.Default.Delete, "Remove Idea", onRemove, needsConfirmation = true
            )
        ), onDismiss = { isDialogOpen.value = false })
    }
}