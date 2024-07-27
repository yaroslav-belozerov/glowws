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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
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
    status: Pair<String?, InferenceManagerState>,
    error: Exception?
) {
    LazyColumn(modifier = modifier) {
        item {
            Text(
                text = stringResource(id = if (status.second.notBusy()) R.string.s_cat_ai else status.second.resId),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillParentMaxWidth()
                    .padding(0.dp, 0.dp, 0.dp, 16.dp),
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
            Row(modifier = Modifier.fillParentMaxWidth()
                .background(if (m.isChosen) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.background)
                .then(if (status.second.notBusy()) Modifier.clickable {
                    if (m.isChosen) {
                        onUnload()
                    } else {
                        onChoose(m.fileName)
                    }
                }
                else Modifier)
                .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = m.name
                    )
                    if (m.isChosen) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(enabled = status.second.notBusy(), onClick = { onDelete(m.fileName) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "delete model button"
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(enabled = status.second.notBusy(), onClick = { onRefresh() }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                }
                OutlinedButton(
                    enabled = status.second.notBusy(),
                    onClick = { onAdd() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Import model"
                    )
                }
            }
        }
    }
}