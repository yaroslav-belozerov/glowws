package com.yaabelozerov.glowws.ui.screen.archive

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.data.local.room.PointType
import com.yaabelozerov.glowws.domain.model.IdeaDomainModel
import com.yaabelozerov.glowws.domain.model.PointDomainModel
import com.yaabelozerov.glowws.domain.model.SettingDomainModel
import com.yaabelozerov.glowws.ui.common.ScreenDialog
import com.yaabelozerov.glowws.ui.model.DialogEntry
import com.yaabelozerov.glowws.ui.model.SelectionState
import com.yaabelozerov.glowws.ui.screen.main.boolean
import java.io.File

@Composable
fun ArchiveScreen(
    modifier: Modifier = Modifier,
    ideas: List<IdeaDomainModel> = emptyList(),
    onClick: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onUnarchive: (Long) -> Unit,
    onSelect: (Long) -> Unit,
    selectionState: SelectionState<Long>,
    settings: Map<SettingsKeys, SettingDomainModel>
) {
    LazyVerticalStaggeredGrid(
        modifier = modifier,
        columns = StaggeredGridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalItemSpacing = 16.dp,
        contentPadding = PaddingValues(16.dp)
    ) {
        items(ideas, key = { it.id }) {
            ArchiveIdea(
                modifier = Modifier.animateItem(),
                previewPoint = it.mainPoint,
                onClick = { onClick(it.id) },
                onRemove = { onRemove(it.id) },
                onUnarchive = { onUnarchive(it.id) },
                onSelect = {
                    onSelect(it.id)
                },
                inSelectionMode = selectionState.inSelectionMode,
                isSelected = selectionState.entries.contains(it.id),
                fullImage = settings[SettingsKeys.IMAGE_FULL_HEIGHT].boolean(),
                displayPlaceholders = settings[SettingsKeys.SHOW_PLACEHOLDERS].boolean()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArchiveIdea(
    modifier: Modifier = Modifier,
    previewPoint: PointDomainModel,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onUnarchive: () -> Unit,
    onSelect: () -> Unit,
    inSelectionMode: Boolean,
    isSelected: Boolean,
    fullImage: Boolean,
    displayPlaceholders: Boolean
) {
    var isDialogOpen by remember { mutableStateOf(false) }

    Card(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .then(
                if (isSelected) {
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.shapes.medium
                    )
                } else {
                    Modifier
                }
            )
            .combinedClickable(
                onClick = if (inSelectionMode) onSelect else onClick,
                onLongClick = { if (!inSelectionMode) isDialogOpen = true }
            )
    ) {
        when (previewPoint.type) {
            PointType.TEXT -> Text(
                text = if (previewPoint.content.isBlank() && displayPlaceholders) {
                    stringResource(id = R.string.placeholder_noname)
                } else {
                    previewPoint.content
                },
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (previewPoint.content.isBlank()) 0.3f else 1f
                )
            )

            PointType.IMAGE -> SubcomposeAsyncImage(
                modifier = Modifier
                    .padding(16.dp)
                    .then(if (fullImage) Modifier else Modifier.height(128.dp))
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop,
                model = File(previewPoint.content),
                contentDescription = null
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
    }

    if (isDialogOpen) {
        ScreenDialog(
            title = if (previewPoint.type == PointType.TEXT) previewPoint.content else "",
            entries = listOf(
                DialogEntry(
                    Icons.Default.Menu,
                    stringResource(id = R.string.label_select),
                    onSelect
                ),
                DialogEntry(
                    Icons.Default.Refresh,
                    stringResource(id = R.string.a_unarchive),
                    onUnarchive
                ),
                DialogEntry(
                    Icons.Default.Delete,
                    stringResource(id = R.string.a_remove_idea),
                    onRemove,
                    needsConfirmation = true
                )
            ),
            onDismiss = { isDialogOpen = false }
        )
    }
}
