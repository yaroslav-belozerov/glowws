package com.yaabelozerov.glowws.ui.screen.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.common.collect.ImmutableMap
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.data.local.ai.InferenceManagerState
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.data.local.room.Model
import com.yaabelozerov.glowws.domain.model.BooleanSettingDomainModel
import com.yaabelozerov.glowws.domain.model.ChoiceSettingDomainModel
import com.yaabelozerov.glowws.domain.model.DoubleSettingDomainModel
import com.yaabelozerov.glowws.domain.model.MultipleChoiceSettingDomainModel
import com.yaabelozerov.glowws.domain.model.SettingDomainModel
import com.yaabelozerov.glowws.domain.model.StringSettingDomainModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
  modifier: Modifier = Modifier,
  svm: SettingsScreenViewModel,
  onModify: (SettingsKeys, String) -> Unit,
  aiStatus: Triple<Model?, InferenceManagerState, Long>,
  onNavigateToAi: () -> Unit
) {
  val settings = svm.state.collectAsState().value
  val changed by svm.settingsChanged.collectAsState()
  val onReset = { svm.resetSettings() }
  val onLogout = { svm.logout() }
  val s by remember(settings.values, changed) {
    mutableStateOf(settings.values.groupBy { it.key.category })
  }
  LazyColumn(modifier = modifier, contentPadding = PaddingValues(vertical = 8.dp)) {
    s.forEach { (k, v) ->
      stickyHeader { SettingsHeader(icon = k.icon, name = stringResource(id = k.resId)) }
      items(v) { entry ->
        when (entry) {
          is BooleanSettingDomainModel -> BooleanSettingsEntry(entry = entry, onModify = onModify)

          is StringSettingDomainModel -> StringSettingsEntry(entry = entry, onModify = onModify)

          is DoubleSettingDomainModel -> {
            var isBeingModified by remember { mutableStateOf(false) }
            Row(
              modifier = Modifier.fillParentMaxWidth().padding(16.dp, 16.dp, 32.dp, 16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              var newValue by remember { mutableStateOf(entry.value.toInt()) }
              Text(stringResource(entry.key.resId), fontSize = 20.sp)
              if (!isBeingModified) {
                Text(
                  newValue.toString(),
                  modifier = Modifier.clickable { isBeingModified = true },
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Bold
                )
              } else {
                OutlinedTextField(
                  value = newValue.toString(),
                  onValueChange = { newValue = it.toInt() },
                  singleLine = true,
                  keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number, imeAction = ImeAction.Done
                  ),
                  keyboardActions = KeyboardActions {
                    onModify(entry.key, newValue.toString())
                    isBeingModified = false
                  })
              }
            }
          }

          is ChoiceSettingDomainModel -> ChoiceSettingDomainEntry(Modifier.fillParentMaxWidth(), entry, onModify)

          is MultipleChoiceSettingDomainModel -> MultipleChoiceSettingsEntry(
            Modifier.fillParentMaxWidth(),
            entry,
            onModify
          )
        }
      }
    }
    stickyHeader { SettingsHeader(icon = Icons.Default.AutoAwesome, name = stringResource(id = R.string.s_cat_ai)) }
    item {
      AiSettingsEntry(status = aiStatus.second, modelName = aiStatus.first?.name) {
        onNavigateToAi()
      }
    }
    item {
      Column(
        modifier = Modifier.fillParentMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        if (changed) {
          OutlinedButton(onClick = onReset) { Text(stringResource(R.string.s_reset)) }
        }
        OutlinedButton(onClick = onLogout) {
          val login by svm.login.collectAsState("")
          Text(stringResource(R.string.log_out) + ": $login")
        }
      }
    }
  }
}
