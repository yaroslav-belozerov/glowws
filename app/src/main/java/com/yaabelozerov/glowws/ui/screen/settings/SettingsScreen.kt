package com.yaabelozerov.glowws.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsCategories
import com.yaabelozerov.glowws.domain.model.BooleanSettingDomainModel
import com.yaabelozerov.glowws.domain.model.ChoiceSettingDomainModel
import com.yaabelozerov.glowws.domain.model.DoubleSettingDomainModel
import com.yaabelozerov.glowws.domain.model.MultipleChoiceSettingDomainModel
import com.yaabelozerov.glowws.domain.model.SettingDomainModel
import com.yaabelozerov.glowws.domain.model.StringSettingDomainModel

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    settings: Map<SettingsCategories, List<SettingDomainModel>>,
    onModify: (SettingsKeys, String) -> Unit,
    aiStatus: Pair<String?, InferenceManagerState>,
    onNavigateToAi: () -> Unit
) {
    LazyColumn(
        modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(settings.keys.toList(), key = { it }) { key ->
            SettingsHeader(icon = key.icon, name = stringResource(id = key.resId))
            settings[key]!!.forEach { entry ->
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
                        Text(text = entry.value.toString())
                        Button(onClick = { /*TODO*/ }) {
                            Text(text = stringResource(id = R.string.label_edit))
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
            AiSettingsEntry(status = aiStatus.second, modelName = aiStatus.first) {
                onNavigateToAi()
            }
        }
        item {
            FeedbackSettingsEntry()
        }
    }
}
