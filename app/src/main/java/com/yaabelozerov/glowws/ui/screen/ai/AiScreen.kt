package com.yaabelozerov.glowws.ui.screen.ai

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.data.InferenceOp
import com.yaabelozerov.glowws.data.busy
import com.yaabelozerov.glowws.data.local.room.Model
import com.yaabelozerov.glowws.data.local.room.ModelType
import com.yaabelozerov.glowws.data.notBusy

@Composable
fun AiScreen(
  modifier: Modifier = Modifier, aivm: AiScreenViewModel
) {
  val state by aivm.aiStatus.collectAsState()
  Column(
    modifier = modifier.animateContentSize().fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    state.models.forEach { model ->
      Row(
        modifier = Modifier.fillMaxWidth().background(
          if (model.isChosen) {
            MaterialTheme.colorScheme.surfaceContainer
          } else {
            MaterialTheme.colorScheme.background
          }
        ).then(
          if (state.operation.notBusy()) {
            Modifier.clickable {
              if (model.isChosen && state.operation == InferenceOp.Ready) {
                aivm.onEvent(AiScreenEvent.Unload)
              } else {
                aivm.onEvent(AiScreenEvent.Choose(model))
              }
            }
          } else {
            Modifier
          }).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
          modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(text = model.name.toString())
            if (model.type is ModelType.Downloadable) {
              Text(
                if (model.type.isDownloaded) "Downloaded" else "Not downloaded",
                style = MaterialTheme.typography.labelMedium
              )
            }
          }
          if (model.isChosen && state.operation == InferenceOp.Ready) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
              imageVector = Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary
            )
          }
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (model.type == ModelType.OnDevice || (model.type is ModelType.Downloadable && model.type.isDownloaded)) {
          IconButton(
            enabled = state.operation.notBusy(), onClick = { aivm.onEvent(AiScreenEvent.Delete(model)) }) {
            Icon(
              imageVector = Icons.Default.Delete, contentDescription = "delete model button"
            )
          }
        }
      }
    }
    Row(
      modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      IconButton(
        enabled = state.operation.notBusy(), onClick = { aivm.onEvent(AiScreenEvent.Refresh) }) {
        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
      }
      OutlinedButton(
        enabled = state.operation.notBusy(),
        onClick = { aivm.onEvent(AiScreenEvent.Add) },
        modifier = Modifier.weight(1f)
      ) {
        if (state.operation.notBusy()) {
          Text(text = stringResource(R.string.ai_import_local))
        } else {
          Row(
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            val op = state.operation
            if (op is InferenceOp.Downloading) {
              CircularProgressIndicator(
                progress = { op.progress },
                modifier = Modifier.size(20.dp),
                strokeCap = StrokeCap.Round,
                color = MaterialTheme.colorScheme.primary
              )
            } else if (op.busy()) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp), strokeCap = StrokeCap.Round, color = MaterialTheme.colorScheme.primary
              )
            }
            Text(
              text = stringResource(id = state.operation.resId) + " " + (state.selected?.name.orEmpty())
            )
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
