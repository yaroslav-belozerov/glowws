package com.yaabelozerov.glowws.ui.screen.idea

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.VerticalAlignmentLine
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.data.local.ai.InferenceManagerState
import com.yaabelozerov.glowws.data.local.ai.notBusy
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.data.local.room.Model
import com.yaabelozerov.glowws.data.local.room.PointType
import com.yaabelozerov.glowws.domain.model.PointDomainModel
import com.yaabelozerov.glowws.domain.model.SettingDomainModel
import com.yaabelozerov.glowws.ui.screen.main.boolean
import com.yaabelozerov.glowws.ui.theme.Typography
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IdeaScreen(
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader,
    points: List<PointDomainModel>,
    onBack: () -> Unit,
    onAdd: (PointType, Long) -> Unit,
    onSave: (Long, String, Boolean) -> Unit,
    onRemove: (Long) -> Unit,
    onExecute: (Long, String) -> Unit,
    settings: Map<SettingsKeys, SettingDomainModel>,
    aiStatus: Triple<Model?, InferenceManagerState, Long>
) {
    BackHandler {
        onBack()
    }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(-1) {
            Row {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "back button",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                AddPointLine(
                    onAdd = { onAdd(it, 0) },
                    holdForType = settings[SettingsKeys.LONG_PRESS_TYPE].boolean()
                )
            }
        }

        items(points, key = { it.id }) { point ->
            when (point.type) {
                PointType.TEXT -> TextPoint(
                    modifier = Modifier.animateItem(),
                    id = point.id,
                    content = point.content,
                    isMain = point.isMain,
                    onSave = { newText, isMain -> onSave(point.id, newText, isMain) },
                    onRemove = { onRemove(point.id) },
                    onExecute = { onExecute(point.id, point.content) },
                    showPlaceholders = settings[SettingsKeys.SHOW_PLACEHOLDERS].boolean(),
                    status = aiStatus
                )

                PointType.IMAGE -> ImagePoint(imageLoader = imageLoader,
                    content = point.content,
                    isMain = point.isMain,
                    onRemove = {
                        onRemove(point.id)
                    },
                    onSave = { onSave(point.id, point.content, it) })
            }
            Spacer(modifier = Modifier.height(16.dp))
            AddPointLine(
                onAdd = { onAdd(it, points.indexOf(point).toLong() + 1) },
                holdForType = settings[SettingsKeys.LONG_PRESS_TYPE].boolean()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddPointLine(onAdd: (PointType) -> Unit, holdForType: Boolean) {
    var open by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp, 0.dp, 0.dp, 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .height(4.dp)
                .weight(1f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
        )
        val haptic = LocalHapticFeedback.current
        Icon(
            modifier = Modifier
                .padding(4.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .combinedClickable(onClick = {
                    if (holdForType) {
                        onAdd(PointType.TEXT)
                    } else {
                        open = true
                    }
                }, onLongClick = {
                    open = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                })
                .padding(8.dp),
            imageVector = Icons.Default.Add,
            contentDescription = "add point button",
            tint = MaterialTheme.colorScheme.primary
        )
    }

    if (open) {
        ModalBottomSheet(onDismissRequest = { open = false }) {
            LazyVerticalGrid(
                modifier = Modifier.padding(12.dp, 0.dp),
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PointType.entries.toList()) {
                    Button(onClick = {
                        onAdd(it)
                        open = false
                    }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(it.icon, contentDescription = it.title)
                            Text(it.title)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImagePoint(
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader,
    content: String,
    isMain: Boolean,
    onSave: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    var uiShown by remember { mutableStateOf(false) }
    Crossfade(modifier = modifier, targetState = uiShown) { showUi ->
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .fillMaxWidth()
                .clickable { uiShown = !uiShown }, contentAlignment = Alignment.BottomStart
        ) {
            SubcomposeAsyncImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawWithContent {
                        drawContent()
                        if (showUi) drawRect(color = Color.Black.copy(alpha = 0.5f), size = size)
                    },
                contentScale = ContentScale.FillWidth,
                model = File(content),
                contentDescription = null,
                imageLoader = imageLoader
            )
            if (showUi) {
                Column(modifier = Modifier.padding(16.dp)) {
                    FilledIconButton(onClick = {
                        onRemove()
                        uiShown = false
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                    }
                    if (isMain) {
                        FilledIconButton(onClick = {
                            onSave(false)
                            uiShown = false
                        }) {
                            Icon(Icons.Default.Star, contentDescription = null)
                        }
                    } else {
                        OutlinedIconButton(onClick = {
                            onSave(true)
                            uiShown = false
                        }) {
                            Icon(Icons.Default.StarOutline, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextPoint(
    modifier: Modifier = Modifier,
    id: Long,
    content: String,
    isMain: Boolean,
    onSave: (String, Boolean) -> Unit,
    onRemove: () -> Unit,
    onExecute: () -> Unit,
    showPlaceholders: Boolean,
    status: Triple<Model?, InferenceManagerState, Long>
) {
    var isBeingModified by remember {
        mutableStateOf(false)
    }
    Crossfade(modifier = modifier, targetState = isMain) { main ->
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .clickable {
                    if (status.third != id) {
                        isBeingModified = !isBeingModified
                    }
                }
                .fillMaxWidth()
                .animateContentSize()
                .background(
                    if (main && !isBeingModified) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    }
                )
                .then(modifier),
        ) {
            if (status.second == InferenceManagerState.RESPONDING && status.third == id) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    backgroundColor = if (main && !isBeingModified) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    }
                )
            }
            val pointFocus = remember {
                FocusRequester()
            }
            if (!isBeingModified) {
                Crossfade(targetState = content) {
                    Text(
                        modifier = Modifier.padding(8.dp),
                        style = Typography.bodyLarge.copy(fontSize = 24.sp, lineHeight = 28.sp),
                        text = if (it.isBlank() && showPlaceholders) {
                            stringResource(
                                id = R.string.placeholder_tap_to_edit
                            )
                        } else {
                            it
                        },
                        color = (if (main) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }).copy(
                                alpha = if (it.isBlank()) 0.2f else 1f
                            )
                    )
                }
            } else {
                Column(Modifier.fillMaxWidth()) {
                    var currentText by remember {
                        mutableStateOf(content)
                    }
                    var currentMainStatus by remember {
                        mutableStateOf(isMain)
                    }
                    LaunchedEffect(key1 = Unit) {
                        pointFocus.requestFocus()
                    }
                    FlowRow(
                        Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = {
                            isBeingModified = false
                        }) {
                            Text(text = stringResource(id = R.string.label_cancel))
                        }
                        if (status.first != null && status.second == InferenceManagerState.ACTIVE) {
                            Log.d("status", status.toString())
                            OutlinedButton(onClick = {
                                isBeingModified = false
                                onSave(currentText, currentMainStatus)
                                onExecute()
                            }) {
                                Text(text = "Rephrase")
                            }
                        }
                        OutlinedButton(onClick = {
                            isBeingModified = false
                            onRemove()
                        }) {
                            Text(text = stringResource(id = R.string.label_remove))
                        }
                        OutlinedButton(onClick = {
                            onSave(currentText, !currentMainStatus)
                            isBeingModified = false
                        }) {
                            Text(
                                text = if (currentMainStatus) {
                                    stringResource(
                                        id = R.string.i_unset_key
                                    )
                                } else {
                                    stringResource(
                                        id = R.string.i_set_key
                                    )
                                }
                            )
                        }
                        Button(onClick = {
                            onSave(currentText, currentMainStatus)
                            isBeingModified = false
                        }) {
                            Text(text = stringResource(id = R.string.label_save))
                        }
                    }
                    TextField(
                        textStyle = Typography.bodyLarge.copy(fontSize = 24.sp, lineHeight = 28.sp),
                        value = currentText,
                        onValueChange = { currentText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(pointFocus)
                    )
                }
            }
        }
    }
}
