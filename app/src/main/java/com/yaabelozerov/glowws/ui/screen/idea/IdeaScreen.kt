package com.yaabelozerov.glowws.ui.screen.idea

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yaabelozerov.glowws.domain.model.PointDomainModel

@Composable
fun IdeaScreen(modifier: Modifier, points: List<PointDomainModel>, onAdd: () -> Unit, onSave: (Long, String) -> Unit, onRemove: (Long) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .then(modifier),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (point in points) {
            AddPointLine(onAdd = onAdd)
            Point(point.content, point.isMain, onSave = { newtext -> onSave(point.id, newtext) }, onRemove = { onRemove(point.id) })
        }
        AddPointLine(onAdd = onAdd)
    }
}

@Composable
fun AddPointLine(onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
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

@Composable
fun Point(text: String, isMain: Boolean, onSave: (String) -> Unit, onRemove: () -> Unit) {
    val isBeingModified = remember {
        mutableStateOf(false)
    }
    val currentText = remember {
        mutableStateOf(text)
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { isBeingModified.value = !isBeingModified.value },
        colors = CardDefaults.cardColors(containerColor = if (isMain) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
    ) {
        if (!isBeingModified.value) Text(
            modifier = Modifier.padding(8.dp), text = text
        ) else Column {
            TextField(value = currentText.value, onValueChange = { currentText.value = it })
            Row {
                Button(onClick = {
                    isBeingModified.value = false
                    currentText.value = text
                }) {
                    Text(text = "Cancel")
                }
                Button(onClick = {
                    onSave(currentText.value)
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
    ), onAdd = {}, onSave = { a,b ->}, onRemove = {})
}