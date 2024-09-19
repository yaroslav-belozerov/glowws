package com.yaabelozerov.glowws.ui.screen.main

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.domain.model.IdeaDomainModel
import com.yaabelozerov.glowws.domain.model.SettingDomainModel
import com.yaabelozerov.glowws.domain.model.findBooleanKey
import com.yaabelozerov.glowws.ui.common.ScreenDialog
import com.yaabelozerov.glowws.ui.model.DialogEntry
import com.yaabelozerov.glowws.ui.theme.Typography

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    ideas: List<IdeaDomainModel> = emptyList(),
    onClickIdea: (Long) -> Unit,
    onArchiveIdea: (Long) -> Unit,
    onSelect: (Long) -> Unit,
    inSelectionMode: Boolean,
    selection: List<Long>,
    settings: List<SettingDomainModel>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(ideas) { idea ->
            Idea(
                modifier = Modifier.animateItem(),
                idea.content,
                idea.modified,
                { onClickIdea(idea.id) },
                { onArchiveIdea(idea.id) },
                { onSelect(idea.id) },
                inSelectionMode,
                selection.contains(idea.id),
                displayPlaceholders = settings.findBooleanKey(SettingsKeys.SHOW_PLACEHOLDERS)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Idea(
    modifier: Modifier = Modifier,
    previewText: String,
    modified: String,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onSelect: () -> Unit,
    inSelectionMode: Boolean,
    isSelected: Boolean,
    displayPlaceholders: Boolean
) {
    var isDialogOpen by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .then(modifier)
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary
                    )
                } else {
                    Modifier
                }
            )
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .combinedClickable(
                onClick = if (!inSelectionMode) {
                    onClick
                } else {
                    onSelect
                },
                onLongClick = { if (!inSelectionMode) isDialogOpen = true }
            )
    ) {
        Text(
            text = if (previewText.isBlank() && displayPlaceholders) {
                stringResource(id = R.string.placeholder_noname)
            } else {
                previewText
            },
            Modifier
                .padding(16.dp)
                .weight(1f),
            style = Typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (previewText.isBlank()) 0.3f else 1f)
        )
        Spacer(modifier = Modifier.width(16.dp))
    }

    if (isDialogOpen) {
        ScreenDialog(
            title = previewText,
            info = listOf(modified),
            entries = listOf(
                DialogEntry(Icons.Default.Menu, stringResource(id = R.string.label_select), {
                    onSelect()
                }),
                DialogEntry(
                    Icons.Default.Delete,
                    stringResource(id = R.string.m_archive_idea),
                    onArchive,
                    needsConfirmation = true
                )
            ),
            onDismiss = { isDialogOpen = false }
        )
    }
}

@Composable
fun TooltipBar(modifier: Modifier = Modifier, message: String, onClick: () -> Unit = {}) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(0.dp, 0.dp, 0.dp, 16.dp)
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(8.dp),
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
fun DialogButton(icon: ImageVector?, text: String, onClick: () -> Unit, isActive: Boolean) {
    if (isActive) {
        Button(onClick = onClick, content = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = text,
                    fontSize = 16.sp
                )
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        })
    } else {
        OutlinedButton(onClick = onClick, content = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = text,
                    fontSize = 16.sp
                )
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        })
    }
}
