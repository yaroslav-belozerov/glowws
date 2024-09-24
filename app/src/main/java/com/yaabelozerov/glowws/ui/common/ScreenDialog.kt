package com.yaabelozerov.glowws.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.ui.model.DialogEntry
import com.yaabelozerov.glowws.ui.screen.main.DialogButton

@Composable
fun ScreenDialog(
    title: String,
    info: List<String> = emptyList(),
    entries: List<DialogEntry>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        var confirm by remember {
            mutableStateOf(List(entries.size) { false })
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 32.sp,
                lineHeight = 32.sp,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                maxLines = 5
            )
            info.forEach { text -> Text(text = text) }
            entries.forEachIndexed { ind, entry ->
                if (!entry.needsConfirmation) {
                    DialogButton(
                        icon = entry.icon,
                        text = entry.name,
                        onClick = {
                            entry.onClick()
                            onDismiss()
                        },
                        isActive = false
                    )
                } else {
                    DialogButton(
                        icon = if (confirm[ind]) {
                            Icons.Default.CheckCircle
                        } else {
                            entry.icon
                        },
                        text = if (confirm[ind]) {
                            stringResource(id = R.string.label_are_you_sure)
                        } else {
                            entry.name
                        },
                        onClick = if (confirm[ind]) {
                            {
                                entry.onClick()
                                onDismiss()
                            }
                        } else {
                            { confirm = List(entries.size) { it == ind } }
                        },
                        isActive = confirm[ind]
                    )
                }
            }
        }
    }
}
