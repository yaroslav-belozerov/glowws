package com.yaabelozerov.glowws.ui.screen.idea

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.domain.model.PointDomainModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IdeaScreen(
    modifier: Modifier,
    points: List<PointDomainModel>,
    onBack: () -> Unit,
    onAdd: (Long) -> Unit,
    onSave: (Long, String, Boolean) -> Unit,
    onRemove: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .then(modifier),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        item {
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

        items(points.size) { ind ->
            Point(text = points[ind].content,
                isMain = points[ind].isMain,
                onSave = { newText, isMain -> onSave(points[ind].id, newText, isMain) },
                onRemove = { onRemove(points[ind].id) })
            Spacer(modifier = Modifier.height(16.dp))
            AddPointLine(onAdd = { onAdd(ind.toLong() + 1) })
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
    onRemove: () -> Unit
) {
    val isBeingModified = remember {
        mutableStateOf(false)
    }
    Crossfade(targetState = isMain) { main ->
        Card(modifier = Modifier
            .fillMaxWidth()
            .clickable { isBeingModified.value = !isBeingModified.value }
            .animateContentSize()
            .then(modifier),
            colors = CardDefaults.cardColors(containerColor = if (main && !isBeingModified.value) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)) {
            if (!isBeingModified.value) Crossfade(targetState = text) {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = it.ifBlank { stringResource(id = R.string.placeholder_unset) },
                    color = (if (main) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface).copy(
                        alpha = if (it.isBlank()) 0.3f else 1f
                    )
                )
            } else Column(Modifier.fillMaxWidth()) {
                val currentText = remember {
                    mutableStateOf(text)
                }
                val currentMainStatus = remember {
                    mutableStateOf(isMain)
                }
                TextField(
                    value = currentText.value,
                    onValueChange = { currentText.value = it },
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = {
                        isBeingModified.value = false
                    }) {
                        Text(text = stringResource(id = R.string.label_cancel))
                    }
                    OutlinedButton(onClick = {
                        isBeingModified.value = false
                        onRemove()
                    }) {
                        Text(text = stringResource(id = R.string.label_remove))
                    }
                    OutlinedButton(onClick = {
                        currentMainStatus.value = !currentMainStatus.value
                    }) {
                        Text(
                            text = if (currentMainStatus.value) stringResource(id = R.string.i_unset_key) else stringResource(
                                id = R.string.i_set_key
                            )
                        )
                    }
                    Button(onClick = {
                        onSave(currentText.value, currentMainStatus.value)
                        isBeingModified.value = false
                    }) {
                        Text(text = stringResource(id = R.string.label_save))
                    }
                }
            }
        }
    }
}