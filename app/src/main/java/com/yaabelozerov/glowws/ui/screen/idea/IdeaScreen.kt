package com.yaabelozerov.glowws.ui.screen.idea

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.data.local.ai.InferenceManagerState
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.data.local.room.Model
import com.yaabelozerov.glowws.data.local.room.ModelType
import com.yaabelozerov.glowws.data.local.room.ModelVariant
import com.yaabelozerov.glowws.data.local.room.PointType
import com.yaabelozerov.glowws.domain.model.PointDomainModel
import com.yaabelozerov.glowws.domain.model.Prompt
import com.yaabelozerov.glowws.domain.model.SettingDomainModel
import com.yaabelozerov.glowws.ui.screen.main.boolean
import java.io.File

@Composable
fun IdeaScreen(
  modifier: Modifier = Modifier,
  points: List<PointDomainModel>,
  onEvent: (IdeaScreenEvent) -> Unit,
  settings: Map<SettingsKeys, SettingDomainModel>,
  aiStatus: Triple<Model?, InferenceManagerState, Long>
) {
  BackHandler { onEvent(IdeaScreenEvent.GoBack) }

  var modifiedId by remember { mutableStateOf<Long?>(null) }
  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .imePadding()
      .background(MaterialTheme.colorScheme.background),
    verticalArrangement = Arrangement.spacedBy(4.dp),
    contentPadding = PaddingValues(16.dp)
  ) {
    item {
      Row {
        IconButton(onClick = { onEvent(IdeaScreenEvent.GoBack) }) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "back button",
            tint = MaterialTheme.colorScheme.primary
          )
        }
        AddPointLine(
          onAdd = { onEvent(IdeaScreenEvent.AddPoint(it, 0)) },
          onExecute = {},
          prompts = emptyList(),
          holdForType = settings[SettingsKeys.LONG_PRESS_TYPE].boolean(),
          aiStatus = Triple(null, InferenceManagerState.IDLE, 0)
        )
      }
    }

    item {
      if (points.isEmpty()) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
        ) {
          Text(stringResource(R.string.i_add_point_placeholder))
          Icon(Icons.Default.ArrowUpward, contentDescription = null)
        }
      }
    }

    items(points, key = { it.id }) { point ->
      when (point.type) {
        PointType.TEXT -> TextPoint(
          modifier = Modifier.animateItem(),
          isBeingModified = modifiedId == point.id,
          onModify = { modifiedId = if (it) point.id else null },
          pt = point,
          onSave = { newText, isMain ->
            onEvent(IdeaScreenEvent.SavePoint(point.id, newText, isMain))
          },
          onRemove = { onEvent(IdeaScreenEvent.RemovePoint(point.id)) },
          onExecute = { prompt, str ->
            onEvent(IdeaScreenEvent.ExecutePoint(prompt, str, point.id))
          },
          onCancel = { onEvent(IdeaScreenEvent.ExecuteCancel) },
          showPlaceholders = settings[SettingsKeys.SHOW_PLACEHOLDERS].boolean(),
          status = aiStatus,
          prompts = listOf(Prompt.Rephrase)
        )

        PointType.IMAGE -> ImagePoint(
          content = point.content,
          isBeingModified = modifiedId == point.id,
          onModify = { modifiedId = if (it) point.id else null },
          isMain = point.isMain,
          onRemove = { onEvent(IdeaScreenEvent.RemovePoint(point.id)) },
          onSave = { onEvent(IdeaScreenEvent.ToggleMain(point.id, !point.isMain)) })
      }
      Spacer(modifier = Modifier.height(4.dp))
      AddPointLine(
        onAdd = { onEvent(IdeaScreenEvent.AddPoint(it, point.index + 1)) },
        holdForType = settings[SettingsKeys.LONG_PRESS_TYPE].boolean(),
        onExecute = {
          onEvent(IdeaScreenEvent.ExecutePointNew(it, points.indexOf(point) + 1))
        },
        prompts = listOfNotNull(
          Prompt.FillIn.takeIf {
          point.type == PointType.TEXT && points.getOrNull(points.indexOf(point) - 1)?.type == PointType.TEXT
        },
          Prompt.Summarize.takeIf { point.type == PointType.TEXT },
          Prompt.Continue.takeIf { point.type == PointType.TEXT }),
        aiStatus = aiStatus
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddPointLine(
  onAdd: (PointType) -> Unit,
  onExecute: (Prompt) -> Unit,
  prompts: List<Prompt>,
  holdForType: Boolean,
  aiStatus: Triple<Model?, InferenceManagerState, Long>
) {
  var open by remember { mutableStateOf(false) }
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .height(4.dp)
        .weight(1f)
        .clip(MaterialTheme.shapes.medium)
        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
    )
    val haptic = LocalHapticFeedback.current
    Icon(
      modifier = Modifier
        .padding(horizontal = 4.dp)
        .clip(MaterialTheme.shapes.extraLarge)
        .combinedClickable(onClick = {
          if (holdForType) {
            onAdd(PointType.TEXT)
          } else {
            open = true
          }
        }, onLongClick = {
          open = true
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        })
        .padding(8.dp),
      imageVector = Icons.Default.Add,
      contentDescription = "add point button",
      tint = MaterialTheme.colorScheme.primary
    )
  }

  if (open) {
    ModalBottomSheet(onDismissRequest = { open = false }) {
      LazyVerticalGrid(
        modifier = Modifier.padding(12.dp, 0.dp),
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(PointType.entries.toList()) {
          FilledTonalButton(
            onClick = {
              onAdd(it)
              open = false
            }) {
            Icon(it.icon, contentDescription = stringResource(it.resId))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(it.resId))
          }
        }
        if (aiStatus.first != null && aiStatus.second == InferenceManagerState.ACTIVE) items(prompts) {
          Button(
            onClick = {
              onExecute(it)
              open = false
            }) {
            Icon(it.icon, contentDescription = stringResource(it.nameRes))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(it.nameRes))
          }
        }
      }
    }
  }
}

@Composable
fun ImagePoint(
  modifier: Modifier = Modifier,
  isBeingModified: Boolean,
  onModify: (Boolean) -> Unit,
  content: String,
  isMain: Boolean,
  onSave: () -> Unit,
  onRemove: () -> Unit
) {
  val bgColor by animateColorAsState(if (isBeingModified) Color.Black.copy(0.5f) else Color.Black.copy(0f))
  Box(
    modifier = modifier
      .clip(MaterialTheme.shapes.medium)
      .fillMaxWidth()
      .clickable {
        onModify(!isBeingModified)
      }, contentAlignment = Alignment.BottomEnd
  ) {
    Text(content)
    AsyncImage(
      modifier = Modifier
        .fillMaxWidth()
        .drawWithContent {
          drawContent()
          drawRect(color = bgColor, size = size)
        }, contentScale = ContentScale.FillWidth, model = content, contentDescription = null
    )
    AnimatedVisibility(
      isBeingModified,
      enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
      exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 })
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Card(
          shape = MaterialTheme.shapes.medium.copy(bottomEnd = CornerSize(4.dp), bottomStart = CornerSize(4.dp)),
          onClick = {
            onRemove()
            onModify(false)
          }) {
          Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.padding(16.dp))
        }
        Card(
          shape = if (!isMain) {
            MaterialTheme.shapes.medium.copy(topEnd = CornerSize(4.dp), topStart = CornerSize(4.dp))
          } else CircleShape, onClick = {
            onSave()
            onModify(false)
          }) {
          Icon(
            Icons.Filled.Star,
            contentDescription = null,
            modifier = Modifier
              .padding(16.dp)
              .scale(if (isMain) 1.2f else 1f)
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextPoint(
  modifier: Modifier = Modifier,
  isBeingModified: Boolean,
  onModify: (Boolean) -> Unit,
  pt: PointDomainModel,
  onSave: (String, Boolean) -> Unit,
  onRemove: () -> Unit,
  onExecute: (Prompt, String) -> Unit,
  onCancel: () -> Unit,
  showPlaceholders: Boolean,
  status: Triple<Model?, InferenceManagerState, Long>,
  prompts: List<Prompt>
) {
  Crossfade(modifier = modifier, targetState = pt.isMain) { main ->
    Box(
      modifier = Modifier
        .clip(MaterialTheme.shapes.medium)
        .then(
          if (status.third != pt.id && !isBeingModified) {
          Modifier.clickable { onModify(true) }
        } else {
          Modifier
        })
        .fillMaxWidth()
        .animateContentSize()
        .background(
          if (main && !isBeingModified) {
            MaterialTheme.colorScheme.primaryContainer
          } else {
            MaterialTheme.colorScheme.surfaceContainer
          }
        )
        .then(modifier),
    ) {
      if (status.second == InferenceManagerState.RESPONDING && status.third == pt.id) {
        LinearProgressIndicator(
          modifier = Modifier.fillMaxWidth(),
          color = MaterialTheme.colorScheme.primary,
          trackColor = if (main && !isBeingModified) {
            MaterialTheme.colorScheme.primaryContainer
          } else {
            MaterialTheme.colorScheme.surfaceContainer
          }
        )
        if (status.first?.type !in ModelType.LOCAL.variants) IconButton(
          onClick = onCancel, modifier = Modifier
            .padding(8.dp)
            .align(Alignment.TopEnd)
        ) { Icon(Icons.Default.Stop, contentDescription = "stop") }
      }
      val pointFocus = remember { FocusRequester() }
      if (!isBeingModified) {
        Crossfade(targetState = pt.content) {
          Text(
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.headlineSmall,
            text = if (it.isBlank() && showPlaceholders) {
              stringResource(id = R.string.placeholder_tap_to_edit)
            } else {
              it
            },
            color = (if (main) {
              MaterialTheme.colorScheme.onPrimaryContainer
            } else {
              MaterialTheme.colorScheme.onSurface
            }).copy(alpha = if (it.isBlank()) 0.2f else 1f)
          )
        }
      } else {
        Column(Modifier.fillMaxWidth()) {
          var currentText by remember(pt.content) { mutableStateOf(TextFieldValue(pt.content)) }
          var currentMainStatus by remember { mutableStateOf(pt.isMain) }
          LaunchedEffect(key1 = Unit) {
            pointFocus.requestFocus()
            currentText = currentText.copy(selection = TextRange(currentText.text.length))
          }
          OutlinedTextField(
            textStyle = MaterialTheme.typography.headlineSmall,
            value = currentText,
            onValueChange = { currentText = it },
            modifier = Modifier
              .fillMaxWidth()
              .focusRequester(pointFocus),
            shape = MaterialTheme.shapes.medium
          )
          FlowRow(
            Modifier
              .fillMaxWidth()
              .padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(onClick = { onModify(false) }) {
              Text(text = stringResource(id = R.string.label_cancel))
            }
            if (status.first != null && status.second == InferenceManagerState.ACTIVE) {
              prompts.forEach {
                OutlinedButton(
                  onClick = {
                    onModify(false)
                    onSave(currentText.text, currentMainStatus)
                    onExecute(it, currentText.text)
                  }) {
                  Icon(
                    it.icon, contentDescription = stringResource(it.nameRes), modifier = Modifier.size(18.dp)
                  )
                  Spacer(Modifier.width(4.dp))
                  Text(text = stringResource(it.nameRes))
                }
              }
            }
            OutlinedIconButton(
              onClick = {
                onModify(false)
                onRemove()
              }) {
              Icon(
                Icons.Default.Delete, contentDescription = stringResource(R.string.a_remove_idea)
              )
            }
            OutlinedIconToggleButton(
              currentMainStatus, onCheckedChange = {
                onSave(currentText.text, it)
                onModify(false)
              }) {
              Icon(
                Icons.Default.Key, contentDescription = stringResource(R.string.i_set_key)
              )
            }
            Button(
              onClick = {
                onSave(currentText.text, currentMainStatus)
                onModify(false)
              }) {
              Text(text = stringResource(id = R.string.label_save))
            }
          }
        }
      }
    }
  }
}

sealed class IdeaScreenEvent {
  data object GoBack : IdeaScreenEvent()

  data class ToggleMain(val id: Long, val isMain: Boolean) : IdeaScreenEvent()

  data class AddPoint(val type: PointType, val index: Long) : IdeaScreenEvent()

  data class SavePoint(val id: Long, val text: String, val isMain: Boolean) : IdeaScreenEvent()

  data class RemovePoint(val id: Long) : IdeaScreenEvent()

  data class ExecutePoint(val prompt: Prompt, val content: String, val pointId: Long) : IdeaScreenEvent()

  data class ExecutePointNew(val prompt: Prompt, val index: Int) : IdeaScreenEvent()

  data object ExecuteCancel : IdeaScreenEvent()
}
