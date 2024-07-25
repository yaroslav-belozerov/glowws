package com.yaabelozerov.glowws.ui.screen.idea

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.domain.model.PointDomainModel
import com.yaabelozerov.glowws.domain.model.SettingDomainModel
import com.yaabelozerov.glowws.domain.model.findBooleanKey
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun IdeaScreen(
    modifier: Modifier,
    points: List<PointDomainModel>,
    onBack: () -> Unit,
    onAdd: (Long) -> Unit,
    onSave: (Long, String, Boolean) -> Unit,
    onRemove: (Long) -> Unit,
    onExecute: (Long, String) -> Unit,
    settings: List<SettingDomainModel>
) {
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
                AddPointLine(onAdd = { onAdd(0) })
            }
        }

        items(points, key = { it.id }) { point ->
            Point(
                modifier = Modifier.animateItem(),
                text = point.content,
                isMain = point.isMain,
                onSave = { newText, isMain -> onSave(point.id, newText, isMain) },
                onRemove = { onRemove(point.id) },
                onExecute = { onExecute(point.id, point.content) },
                showPlaceholders = settings.findBooleanKey(SettingsKeys.SHOW_PLACEHOLDERS)
            )
            Spacer(modifier = Modifier.height(16.dp))
            AddPointLine(onAdd = { onAdd(points.indexOf(point).toLong() + 1) })
        }
    }
}

@Composable
fun AddPointLine(onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp, 0.dp, 0.dp, 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .height(4.dp)
                .weight(1f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
        )
        IconButton(
            onClick = onAdd
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "add point button",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Point(
    modifier: Modifier = Modifier,
    text: String,
    isMain: Boolean,
    onSave: (String, Boolean) -> Unit,
    onRemove: () -> Unit,
    onExecute: () -> Unit,
    showPlaceholders: Boolean
) {
    var isBeingModified by remember {
        mutableStateOf(false)
    }
    Crossfade(modifier = modifier, targetState = isMain) { main ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isBeingModified = !isBeingModified }
                .animateContentSize()
                .then(modifier),
            colors = CardDefaults.cardColors(
                containerColor = if (main && !isBeingModified) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                }
            )
        ) {
            if (!isBeingModified) {
                Crossfade(targetState = text) {
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = if (it.isBlank() && showPlaceholders) {
                            stringResource(
                                id = R.string.placeholder_noname
                            )
                        } else {
                            it
                        },
                        color = (
                            if (main) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                            ).copy(
                            alpha = if (it.isBlank()) 0.3f else 1f
                        )
                    )
                }
            } else {
                Column(Modifier.fillMaxWidth()) {
                    var currentText by remember {
                        mutableStateOf(text)
                    }
                    var currentMainStatus by remember {
                        mutableStateOf(isMain)
                    }
                    TextField(
                        value = currentText,
                        onValueChange = { currentText = it },
                        modifier = Modifier.fillMaxWidth()
                    )
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
                        OutlinedButton(onClick = {
                            isBeingModified = false
                            onExecute()
                        }) {
                            Text(text = "Rephrase .")
                        }
                        OutlinedButton(onClick = {
                            isBeingModified = false
                            onRemove()
                        }) {
                            Text(text = stringResource(id = R.string.label_remove))
                        }
                        OutlinedButton(onClick = {
                            currentMainStatus = !currentMainStatus
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
                }
            }
        }
    }
}
