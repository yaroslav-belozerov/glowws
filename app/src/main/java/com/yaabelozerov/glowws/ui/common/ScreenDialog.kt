package com.yaabelozerov.glowws.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.ui.model.DialogEntry
import com.yaabelozerov.glowws.ui.screen.main.DialogButton

@Composable
fun ScreenDialog(
    title: String,
    info: List<Pair<ImageVector, String>> = emptyList(),
    entries: List<DialogEntry>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        var confirm by remember {
            mutableStateOf(List(entries.size) { false })
        }
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                title.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        fontSize = 26.sp,
                        lineHeight = 26.sp,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 5
                    )
                }
                info.forEach { (icon, text) ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.tertiary)
                        Text(text = text)
                    }
                }
                entries.forEachIndexed { ind, entry ->
                    if (!entry.needsConfirmation) {
                        DialogButton(
                            icon = entry.icon, text = entry.name, onClick = {
                                entry.onClick()
                                onDismiss()
                            }, isActive = false
                        )
                    } else {
                        DialogButton(icon = if (confirm[ind]) {
                            Icons.Default.CheckCircle
                        } else {
                            entry.icon
                        }, text = if (confirm[ind]) {
                            stringResource(id = R.string.label_are_you_sure)
                        } else {
                            entry.name
                        }, onClick = if (confirm[ind]) {
                            {
                                entry.onClick()
                                onDismiss()
                            }
                        } else {
                            { confirm = List(entries.size) { it == ind } }
                        }, isActive = confirm[ind])
                    }
                }
            }
        }
    }
}
