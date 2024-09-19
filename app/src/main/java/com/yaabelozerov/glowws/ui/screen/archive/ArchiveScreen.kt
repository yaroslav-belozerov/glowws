package com.yaabelozerov.glowws.ui.screen.archive

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.yaabelozerov.glowws.domain.model.findBooleanKey
import com.yaabelozerov.glowws.ui.common.ScreenDialog
import com.yaabelozerov.glowws.ui.model.DialogEntry
import com.yaabelozerov.glowws.ui.model.SelectionState
import com.yaabelozerov.glowws.ui.theme.Typography
import java.io.File

@Composable
fun ArchiveScreen(
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader,
    ideas: List<IdeaDomainModel> = emptyList(),
    onClick: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onUnarchive: (Long) -> Unit,
    onSelect: (Long) -> Unit,
    selectionState: SelectionState<Long>,
    settings: List<SettingDomainModel>
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(ideas, key = { it.id }) {
            ArchiveIdea(
                modifier = Modifier.animateItem(),
                imageLoader = imageLoader,
                previewPoint = it.mainPoint,
                onClick = { onClick(it.id) },
                onRemove = { onRemove(it.id) },
                onUnarchive = { onUnarchive(it.id) },
                onSelect = {
                    onSelect(it.id)
                },
                inSelectionMode = selectionState.inSelectionMode,
                isSelected = selectionState.entries.contains(it.id),
                fullImage = settings.findBooleanKey(SettingsKeys.IMAGE_FULL_HEIGHT)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArchiveIdea(
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader,
    previewPoint: PointDomainModel,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onUnarchive: () -> Unit,
    onSelect: () -> Unit,
    inSelectionMode: Boolean,
    isSelected: Boolean,
    fullImage: Boolean
) {
    var isDialogOpen by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier
        .fillMaxWidth()
        .then(
            if (isSelected) {
                Modifier.border(
                    2.dp, MaterialTheme.colorScheme.primary
                )
            } else {
                Modifier
            }
        )
        .background(MaterialTheme.colorScheme.primaryContainer)
        .combinedClickable(onClick = if (inSelectionMode) onSelect else onClick,
            onLongClick = { if (!inSelectionMode) isDialogOpen = true })) {
        when (previewPoint.type) {
            PointType.TEXT -> Text(
                text = previewPoint.content.ifBlank {
                    stringResource(id = R.string.placeholder_noname)
                },
                maxLines = 10,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f),
                style = Typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (previewPoint.content.isBlank()) 0.3f else 1f)
            )

            PointType.IMAGE -> SubcomposeAsyncImage(
                modifier = Modifier
                    .padding(16.dp)
                    .then(if (fullImage) Modifier else Modifier.height(128.dp)).fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.FillWidth,
                model = File(previewPoint.content),
                contentDescription = null,
                imageLoader = imageLoader
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
    }

    if (isDialogOpen) {
        ScreenDialog(title = if (previewPoint.type == PointType.TEXT) previewPoint.content else "",
            entries = listOf(
                DialogEntry(
                    Icons.Default.Menu, stringResource(id = R.string.label_select), onSelect
                ), DialogEntry(
                    Icons.Default.Refresh, stringResource(id = R.string.a_unarchive), onUnarchive
                ), DialogEntry(
                    Icons.Default.Delete,
                    stringResource(id = R.string.a_remove_idea),
                    onRemove,
                    needsConfirmation = true
                )
            ),
            onDismiss = { isDialogOpen = false })
    }
}
