package com.yaabelozerov.glowws.ui.screen.ai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AiScreen(modifier: Modifier = Modifier, models: List<String>, onChoose: (Int) -> Unit, onDelete: (Int) -> Unit, onAdd: () -> Unit, onRefresh: () -> Unit) {
    LazyColumn(modifier = modifier.padding(16.dp)){
        items(models.size) { index ->
            Row(modifier = Modifier.clickable { onChoose(index) }) {
                Text(text = models[index])
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { onDelete(index) }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "delete model button")
                }
            }
        }
        item {
            Row {
                OutlinedButton(onClick = { onRefresh() }) {
                    Text(text = "Refresh")
                }
                Button(onClick = { onAdd() }) {
                    Text(text = "Import model")
                }
            }
        }
    }
}