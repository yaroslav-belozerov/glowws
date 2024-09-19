package com.yaabelozerov.glowws.ui.screen.main

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.yaabelozerov.glowws.ui.theme.Typography
import java.io.File

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader,
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
                imageLoader,
                idea.mainPoint,
                idea.modified.string,
                { onClickIdea(idea.id) },
                { onArchiveIdea(idea.id) },
                { onSelect(idea.id) },
                inSelectionMode,
                selection.contains(idea.id),
                displayPlaceholders = settings.findBooleanKey(SettingsKeys.SHOW_PLACEHOLDERS),
                fullImage = settings.findBooleanKey(SettingsKeys.IMAGE_FULL_HEIGHT)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Idea(
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader,
    previewPoint: PointDomainModel,
    modified: String,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onSelect: () -> Unit,
    inSelectionMode: Boolean,
    isSelected: Boolean,
    displayPlaceholders: Boolean,
    fullImage: Boolean
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
                        2.dp, MaterialTheme.colorScheme.primary
                    )
                } else {
                    Modifier
                }
            )
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .combinedClickable(onClick = if (!inSelectionMode) {
                onClick
            } else {
                onSelect
            }, onLongClick = { if (!inSelectionMode) isDialogOpen = true })
    ) {
        when (previewPoint.type) {
            PointType.TEXT -> Text(text = if (previewPoint.content.isBlank() && displayPlaceholders) {
                stringResource(id = R.string.placeholder_noname)
            } else {
                previewPoint.content
            }, maxLines = 10, overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f),
                style = Typography.bodyLarge.copy(fontSize = 24.sp, lineHeight = 28.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (previewPoint.content.isBlank()) 0.3f else 1f)
            )
            PointType.IMAGE -> SubcomposeAsyncImage(modifier = Modifier
                .padding(16.dp).then(if (fullImage) Modifier else Modifier.height(128.dp))
                .clip(MaterialTheme.shapes.medium).fillMaxWidth(), contentScale = ContentScale.FillWidth, model = File(previewPoint.content), contentDescription = null, imageLoader = imageLoader)
        }
        Spacer(modifier = Modifier.width(16.dp))
    }

    if (isDialogOpen) {
        ScreenDialog(
            title = if (previewPoint.type == PointType.TEXT) previewPoint.content else "",
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
