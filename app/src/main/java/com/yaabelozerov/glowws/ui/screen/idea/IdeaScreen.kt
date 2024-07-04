package com.yaabelozerov.glowws.ui.screen.idea

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yaabelozerov.glowws.domain.model.PointDomainModel

@Composable
fun IdeaScreen(
    modifier: Modifier,
    points: List<PointDomainModel>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
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
                Button(onClick = onBack) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "back button")
                }
                Spacer(modifier = Modifier.weight(1f))
                AddPointLine(onAdd = onAdd)
            }
        }

        items(points) { point ->
            Point(point.content,
                point.isMain,
                onSave = { newText, isMain -> onSave(point.id, newText, isMain) },
                onRemove = { onRemove(point.id) })
            Spacer(modifier = Modifier.height(8.dp))
            AddPointLine(onAdd = onAdd)
        }
    }
}

@Composable
fun AddPointLine(onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .height(4.dp)
                .weight(1f)
        )
        Button(onClick = onAdd) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "add point button")
        }
    }

}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Point(text: String, isMain: Boolean, onSave: (String, Boolean) -> Unit, onRemove: () -> Unit) {
    val isBeingModified = remember {
        mutableStateOf(false)
    }
    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable { isBeingModified.value = !isBeingModified.value }
        .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = if (isMain && !isBeingModified.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)) {
        if (!isBeingModified.value) Text(
            modifier = Modifier.padding(8.dp),
            text = text.ifBlank { "Empty" },
            color = MaterialTheme.colorScheme.primary.copy(alpha = if (text.isBlank()) 0.3f else 1f)
        ) else Column(Modifier.fillMaxWidth()) {
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
                    Text(text = "Cancel")
                }
                OutlinedButton(onClick = {
                    onRemove()
                }) {
                    Text(text = "Remove")
                }
                OutlinedButton(onClick = { currentMainStatus.value = !currentMainStatus.value }) {
                    Text(text = if (currentMainStatus.value) "Set as non-key" else "Set as key")
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(modifier = Modifier.weight(1f), onClick = {
                    onSave(currentText.value, currentMainStatus.value)
                    isBeingModified.value = false
                }) {
                    Text(text = "Save")
                }
            }
        }
    }
}

@Composable
@Preview
fun IdeaScreenPreview() {
    IdeaScreen(modifier = Modifier, points = listOf(
        PointDomainModel(0, "test_point1", false),
        PointDomainModel(0, "test_point2", true),
        PointDomainModel(0, "test_point2", false),
    ), onBack = {}, onAdd = {}, onSave = { a, b, c -> }, onRemove = {})
}