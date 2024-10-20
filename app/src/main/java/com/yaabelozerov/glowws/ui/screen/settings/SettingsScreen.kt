package com.yaabelozerov.glowws.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    settings: Map<SettingsKeys, SettingDomainModel>,
    onModify: (SettingsKeys, String) -> Unit,
    aiStatus: Triple<Model?, InferenceManagerState, Long>,
    onNavigateToAi: () -> Unit
) {
    val s by remember {
        mutableStateOf(settings.values.groupBy { it.key.category })
    }
    LazyColumn(
        modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(s.keys.toList().sorted()) { key ->
            SettingsHeader(icon = key.icon, name = stringResource(id = key.resId))
            s[key]!!.forEach { entry ->
                when (entry) {
                    is BooleanSettingDomainModel -> BooleanSettingsEntry(
                        entry = entry, onModify = onModify
                    )

                    is StringSettingDomainModel -> {
                        Text(text = entry.value)
                        Button(onClick = { /*TODO*/ }) {
                            Text(text = stringResource(id = R.string.label_edit))
                        }
                    }

                    is DoubleSettingDomainModel -> {
                        var isBeingModifier by remember {
                            mutableStateOf(false)
                        }
                        Row(
                            modifier = Modifier.fillParentMaxWidth().padding(16.dp, 16.dp, 32.dp, 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            var newValue by remember {
                                mutableStateOf(entry.value.toInt())
                            }
                            Text(stringResource(entry.key.resId), fontSize = 20.sp)
                            if (!isBeingModifier) Text(newValue.toString(),
                                modifier = Modifier.clickable {
                                    isBeingModifier = true
                                }, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            else {
                                OutlinedTextField(value = newValue.toString(),
                                    onValueChange = { newValue = it.toInt() },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions.Default.copy(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions {
                                        onModify(entry.key, newValue.toString())
                                        isBeingModifier = false
                                    })
                            }
                        }
                    }

                    is ChoiceSettingDomainModel -> ChoiceSettingDomainEntry(
                        Modifier.fillParentMaxWidth(), entry, onModify
                    )

                    is MultipleChoiceSettingDomainModel -> MultipleChoiceSettingsEntry(
                        Modifier.fillParentMaxWidth(), entry, onModify
                    )
                }
            }
        }
        item {
            AiSettingsEntry(status = aiStatus.second, modelName = aiStatus.first?.name) {
                onNavigateToAi()
            }
        }
        item {
            FeedbackSettingsEntry()
        }
    }
}
