package com.yaabelozerov.glowws.ui.screen.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.data.local.ai.InferenceManagerState
import com.yaabelozerov.glowws.data.local.ai.notBusy
import com.yaabelozerov.glowws.data.local.room.Model
import com.yaabelozerov.glowws.data.local.room.ModelType
import com.yaabelozerov.glowws.data.local.room.ModelVariant

@Composable
fun AiScreen(
    modifier: Modifier = Modifier,
    models: Map<ModelType, List<Model>>,
    onEvent: (AiScreenEvent) -> Unit,
    status: Triple<Model?, InferenceManagerState, Long>,
    error: Exception?
) {
  Column(
      modifier = modifier.animateContentSize().fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(8.dp)) {
        error?.let {
          Text(modifier = Modifier.padding(16.dp), text = it.localizedMessage ?: stringResource(R.string.error_unknown))
        }
        models.keys.forEach { type ->
          Text(
              text = stringResource(type.resId),
              modifier = Modifier.padding(16.dp),
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp)
          models[type]?.forEach { model ->
            Column(modifier = Modifier.fillMaxWidth()) {
              var open by remember { mutableStateOf(false) }
              Row(
                  modifier =
                      Modifier.fillMaxWidth()
                          .background(
                              if (model.isChosen) {
                                MaterialTheme.colorScheme.surfaceContainer
                              } else {
                                MaterialTheme.colorScheme.background
                              })
                          .then(
                              if (status.second == InferenceManagerState.ACTIVE ||
                                  status.second == InferenceManagerState.IDLE) {
                                Modifier.clickable {
                                  if (model.isChosen &&
                                      status.second == InferenceManagerState.ACTIVE) {
                                    onEvent(AiScreenEvent.Unload)
                                  } else {
                                    onEvent(AiScreenEvent.Choose(model))
                                  }
                                }
                              } else {
                                Modifier
                              })
                          .padding(16.dp),
                  verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically) {
                          Text(text = model.name.toString())
                          if (model.isChosen && status.second == InferenceManagerState.ACTIVE) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary)
                          }
                        }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (model.type == ModelVariant.ONDEVICE) {
                      IconButton(
                          enabled = status.second.notBusy(),
                          onClick = { onEvent(AiScreenEvent.Delete(model)) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "delete model button")
                          }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (model.type.needsToken && (model.token.orEmpty()).isBlank()) {
                      Text(
                          stringResource(R.string.ai_no_token),
                          color = MaterialTheme.colorScheme.error)
                    }
                    IconButton(enabled = status.second.notBusy(), onClick = { open = !open }) {
                      Icon(
                          imageVector =
                              if (!open) {
                                Icons.Default.KeyboardArrowDown
                              } else {
                                Icons.Default.KeyboardArrowUp
                              },
                          contentDescription = "expand model settings")
                    }
                  }
              AnimatedVisibility(
                  open,
                  enter = slideInVertically() + fadeIn() + expandVertically(),
                  exit = slideOutVertically() + fadeOut() + shrinkVertically()) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                          var newName by remember { mutableStateOf(model.name.orEmpty()) }
                          var nameChanged by remember { mutableStateOf(false) }
                          val focus = remember { FocusRequester() }
                          val focusManager = LocalFocusManager.current

                          OutlinedTextField(
                              shape = MaterialTheme.shapes.medium,
                              trailingIcon = {
                                if (nameChanged) {
                                  IconButton(
                                      onClick = {
                                        onEvent(AiScreenEvent.Edit(model.copy(name = newName)))
                                        focusManager.clearFocus()
                                        nameChanged = false
                                      }) {
                                        Icon(Icons.Default.Check, "OK")
                                      }
                                }
                              },
                              prefix = {
                                Text(stringResource(R.string.ai_model_name), modifier = Modifier.padding(0.dp, 0.dp, 8.dp, 0.dp))
                              },
                              singleLine = true,
                              modifier = Modifier.fillMaxWidth().focusRequester(focus),
                              value = newName,
                              onValueChange = {
                                newName = it
                                nameChanged = newName != model.name
                              })

                            if (model.type.needsToken) {
                                var token by remember { mutableStateOf(model.token ?: "") }
                                var changed by remember { mutableStateOf(false) }
                                OutlinedTextField(shape = MaterialTheme.shapes.medium,
                                    trailingIcon = {
                                        if (changed) IconButton(onClick = {
                                            onEvent(AiScreenEvent.Edit(model.copy(token = token)))
                                            focusManager.clearFocus()
                                            changed = false
                                        }) {
                                            Icon(
                                                Icons.Default.Check, "OK"
                                            )
                                        }
                                    },
                                    prefix = {
                                        Text(
                                            stringResource(R.string.ai_model_token),
                                            modifier = Modifier.padding(0.dp, 0.dp, 8.dp, 0.dp)
                                        )
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focus),
                                    value = token,
                                    onValueChange = {
                                        token = it
                                        changed = token != model.token
                                    },
                                    visualTransformation = PasswordVisualTransformation()
                                )
                            }
                        }
                  }
            }
          }
        }
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              IconButton(
                  enabled = status.second.notBusy(), onClick = { onEvent(AiScreenEvent.Refresh) }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                  }
              OutlinedButton(
                  enabled = status.second.notBusy(),
                  onClick = { onEvent(AiScreenEvent.Add) },
                  modifier = Modifier.weight(1f)) {
                    if (status.second.notBusy()) {
                      Text(text = stringResource(R.string.ai_import_local))
                    } else {
                      Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!status.second.notBusy()) {
                              CircularProgressIndicator(
                                  modifier = Modifier.size(20.dp),
                                  strokeCap = StrokeCap.Round,
                                  color = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                text =
                                    stringResource(id = status.second.resId) +
                                        " " +
                                        (status.first?.name.orEmpty()))
                          }
                    }
                  }
            }
      }
}

sealed class AiScreenEvent {
  data class Choose(val model: Model) : AiScreenEvent()

  data class Delete(val model: Model) : AiScreenEvent()

  data object Unload : AiScreenEvent()

  data object Add : AiScreenEvent()

  data object Refresh : AiScreenEvent()

  data class Edit(val model: Model) : AiScreenEvent()
}
