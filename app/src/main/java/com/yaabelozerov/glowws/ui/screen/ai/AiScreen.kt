package com.yaabelozerov.glowws.ui.screen.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaabelozerov.glowws.data.local.ai.InferenceManagerState
import com.yaabelozerov.glowws.data.local.ai.notBusy
import com.yaabelozerov.glowws.data.local.room.Model
import com.yaabelozerov.glowws.data.local.room.ModelType

@Composable
fun AiScreen(
    modifier: Modifier = Modifier,
    models: Map<ModelType, List<Model>>,
    onChoose: (Model) -> Unit,
    onDelete: (Model) -> Unit,
    onUnload: () -> Unit,
    onAdd: () -> Unit,
    onRefresh: () -> Unit,
    onEdit: (Model) -> Unit,
    status: Triple<Model?, InferenceManagerState, Long>,
    error: Exception?
) {
    LazyColumn(modifier = modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        error?.let {
            item {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = it.localizedMessage ?: "Unknown error"
                )
            }
        }
        items(models.keys.toList()) { type ->
            Text(
                text = stringResource(type.resId),
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            models[type]?.forEach { model ->
                Column(modifier = Modifier.fillParentMaxWidth().animateContentSize()) {
                    var open by remember { mutableStateOf(false) }
                    Row(modifier = Modifier
                        .fillParentMaxWidth()
                        .background(
                            if (model.isChosen) MaterialTheme.colorScheme.surfaceContainer
                            else MaterialTheme.colorScheme.background
                        )
                        .then(if (status.second == InferenceManagerState.ACTIVE || status.second == InferenceManagerState.IDLE) {
                            Modifier.clickable {
                                if (model.isChosen && status.second == InferenceManagerState.ACTIVE) {
                                    onUnload()
                                } else {
                                    onChoose(model)
                                }
                            }
                        } else {
                            Modifier
                        })
                        .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = model.name.toString()
                            )
                            if (model.isChosen && status.second == InferenceManagerState.ACTIVE) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(enabled = status.second.notBusy(),
                            onClick = { onDelete(model) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "delete model button"
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(enabled = status.second.notBusy(),
                            onClick = { open = !open }) {
                            Icon(
                                imageVector = if (!open) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = "delete model button"
                            )
                        }
                    }
                    AnimatedVisibility(open, enter = slideInVertically() + fadeIn() + expandVertically(), exit = slideOutVertically() + fadeOut() + shrinkVertically()) {
                        Column(modifier = Modifier.padding(16.dp).fillParentMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            var newName by remember { mutableStateOf(model.name ?: "") }
                            var nameChanged by rememberSaveable { mutableStateOf(false) }
                            val focus = remember { FocusRequester() }
                            val focusManager = LocalFocusManager.current

                            OutlinedTextField(trailingIcon = {
                                if (nameChanged) IconButton(onClick = {
                                    onEdit(model.copy(name = newName))
                                    focusManager.clearFocus()
                                }) {
                                    Icon(
                                        Icons.Default.Check, "OK"
                                    )
                                }
                            },
                                prefix = { Text("Name", modifier = Modifier.padding(0.dp, 0.dp, 8.dp, 0.dp)) },
                                singleLine = true,
                                modifier = Modifier.fillParentMaxWidth().padding(16.dp, 0.dp).focusRequester(focus),
                                value = newName,
                                onValueChange = {
                                    newName = it
                                    nameChanged = newName != model.name
                                })

                            if (type.needsToken) {
                                var token by remember { mutableStateOf(model.token ?: "") }
                                var changed by remember { mutableStateOf(false) }
                                OutlinedTextField(trailingIcon = {
                                    if (changed) IconButton(onClick = {
                                        onEdit(model.copy(token = token))
                                        focusManager.clearFocus()
                                    }) {
                                        Icon(
                                            Icons.Default.Check, "OK"
                                        )
                                    }
                                },
                                    prefix = { Text("Token", modifier = Modifier.padding(0.dp, 0.dp, 8.dp, 0.dp)) },
                                    singleLine = true,
                                    modifier = Modifier.fillParentMaxWidth().padding(16.dp, 0.dp).focusRequester(focus),
                                    value = token,
                                    onValueChange = {
                                        token = it
                                        changed = token != model.token
                                    }, visualTransformation = PasswordVisualTransformation())
                            }
                        }
                    }
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
                    if (status.second.notBusy()) {
                        Text(
                            text = "Import model"
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!status.second.notBusy()) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeCap = StrokeCap.Round,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = stringResource(id = status.second.resId) + " " + (status.first?.name
                                    ?: "")
                            )
                        }
                    }
                }
            }
        }
    }
}