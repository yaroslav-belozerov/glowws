package com.yaabelozerov.glowws.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaabelozerov.glowws.Const.String.JSON_DELIMITER
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.data.local.ai.InferenceManagerState
import com.yaabelozerov.glowws.data.local.ai.notBusy
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.domain.model.BooleanSettingDomainModel
import com.yaabelozerov.glowws.domain.model.ChoiceSettingDomainModel
import com.yaabelozerov.glowws.domain.model.MultipleChoiceSettingDomainModel
import com.yaabelozerov.glowws.toReadableKey

@Composable
fun BooleanSettingsEntry(
    modifier: Modifier = Modifier,
    entry: BooleanSettingDomainModel,
    onModify: (SettingsKeys, String) -> Unit
) {
  var checked by remember { mutableStateOf(entry.value) }
  Row(
      modifier =
          modifier
              .clickable {
                checked = !checked
                onModify(entry.key, checked.toString())
              }
              .padding(16.dp, 4.dp),
      verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(entry.key.resId),
            fontSize = 20.sp,
            modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = {
              checked = !checked
              onModify(entry.key, checked.toString())
            },
            modifier = Modifier.padding(0.dp, 0.dp, 8.dp, 0.dp))
      }
}

@Composable
fun ChoiceSettingDomainEntry(
    modifier: Modifier = Modifier,
    entry: ChoiceSettingDomainModel,
    onModify: (SettingsKeys, String) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  Column(modifier = modifier.clickable { expanded = true }.padding(16.dp, 16.dp)) {
    Text(text = stringResource(entry.key.resId), fontSize = 24.sp)
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      entry.choices.forEachIndexed { index, value ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.fillMaxWidth().clickable {
                  onModify(entry.key, value)
                  expanded = false
                }) {
              val local = entry.localChoicesIds.getOrNull(index)
              if (entry.value == value) {
                Icon(
                    modifier = Modifier.padding(16.dp, 0.dp, 0.dp, 0.dp),
                    imageVector = Icons.Default.Check,
                    contentDescription = null)
              }
              Text(
                  text = if (local != null) stringResource(id = local) else value.toReadableKey(),
                  fontWeight = if (value == entry.value) FontWeight.Bold else FontWeight.Normal,
                  modifier =
                      Modifier.padding(
                          if (entry.value != value) 16.dp else 8.dp, 8.dp, 16.dp, 8.dp))
            }
      }
    }
    val local = entry.localChoicesIds.getOrNull(entry.choices.indexOf(entry.value))
    Text(text = if (local != null) stringResource(id = local) else entry.value.toReadableKey())
  }
}

@Composable
fun MultipleChoiceSettingsEntry(
    modifier: Modifier = Modifier,
    entry: MultipleChoiceSettingDomainModel,
    onModify: (SettingsKeys, String) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  Column(modifier = modifier.clickable { expanded = true }.padding(16.dp, 16.dp)) {
    Text(text = stringResource(entry.key.resId), fontSize = 24.sp)
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      entry.choices.forEachIndexed { index, elem ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.fillMaxWidth().clickable {
                  onModify(
                      entry.key,
                      entry.value
                          .mapIndexed { i, value -> if (i == index) !value else value }
                          .joinToString(JSON_DELIMITER))
                }) {
              if (entry.value[index]) {
                Icon(
                    modifier = Modifier.padding(16.dp, 0.dp, 0.dp, 0.dp),
                    imageVector = Icons.Default.Check,
                    contentDescription = null)
              }
              val local = entry.localChoicesIds.getOrNull(index)
              Text(
                  text =
                      if (local != null) {
                        stringResource(id = local)
                      } else {
                        elem.toReadableKey()
                      },
                  modifier = Modifier.padding(16.dp, 8.dp))
            }
      }
    }
    Text(
        text =
            if (entry.value.all { !it }) {
              stringResource(id = R.string.placeholder_null)
            } else {
              entry.choices
                  .mapIndexed { index, s ->
                    val local = entry.localChoicesIds.getOrNull(index)
                    if (local != null) stringResource(id = local) else s.toReadableKey()
                  }
                  .joinToString(", ")
            })
  }
}

@Composable
fun AiSettingsEntry(
    modifier: Modifier = Modifier,
    status: InferenceManagerState,
    modelName: String?,
    onNavigate: () -> Unit
) {
  Column(modifier = modifier.fillMaxWidth()) {
    SettingsHeader(icon = Icons.Default.AutoAwesome, name = stringResource(id = R.string.s_cat_ai))
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onNavigate() }.padding(16.dp, 8.dp)) {
          Column(Modifier.weight(1f)) {
            if (!modelName.isNullOrBlank()) {
              Text(text = modelName, fontSize = 20.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                  text = stringResource(id = status.resId) + if (status.notBusy()) "" else "...",
                  fontSize = if (!modelName.isNullOrBlank()) 16.sp else 20.sp)
              if (status == InferenceManagerState.ACTIVE) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
              }
            }
          }
          OutlinedButton(onClick = { onNavigate() }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(text = stringResource(id = R.string.label_edit))
              Icon(
                  imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                  contentDescription = null)
            }
          }
        }
  }
}

@Composable
fun SettingsHeader(icon: ImageVector, name: String) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 16.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = "$name icon",
            tint = MaterialTheme.colorScheme.primary)
        Text(text = name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
      }
}
