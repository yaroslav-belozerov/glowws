package com.yaabelozerov.glowws.ui.screen.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.data.local.ai.InferenceManagerState

data class AiModel(
    val name: String, val fileName: String, val isChosen: Boolean
)

@Composable
fun AiScreen(
    modifier: Modifier = Modifier,
    models: List<AiModel>,
    onChoose: (String) -> Unit,
    onDelete: (String) -> Unit,
    onUnload: () -> Unit,
    onAdd: () -> Unit,
    onRefresh: () -> Unit,
    status: InferenceManagerState,
    error: Exception?
) {
    LazyColumn(modifier = modifier) {
        item {
            Text(
                text = when (status) {
                    InferenceManagerState.IDLE -> stringResource(id = R.string.s_cat_ai)
                    InferenceManagerState.LOADING -> "Loading..."
                    InferenceManagerState.ACTIVATING -> "Activating..."
                    InferenceManagerState.REMOVING -> "Removing..."
                },
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillParentMaxWidth().padding(0.dp, 0.dp, 0.dp, 16.dp),
                textAlign = TextAlign.Center
            )
        }
        error?.let {
            item {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = it.localizedMessage ?: "Unknown error"
                )
            }
        }
        items(models) { m ->
            Row(modifier = Modifier
                .background(if (m.isChosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background)
                .then(if (status.isIdle()) Modifier.clickable {
                    if (m.isChosen) {
                        onUnload()
                    } else {
                        onChoose(m.fileName)
                    }
                }
                else Modifier)
                .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = m.name, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(enabled = status.isIdle(), onClick = { onDelete(m.fileName) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "delete model button"
                    )
                }
            }
        }
        item {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = status.isIdle(), onClick = { onRefresh() }, modifier = Modifier.weight(1f)) {
                    Text(text = "Refresh")
                }
                Button(enabled = status.isIdle(), onClick = { onAdd() }, modifier = Modifier.weight(1f)) {
                    Text(text = "Import model")
                }
            }
        }
    }
}