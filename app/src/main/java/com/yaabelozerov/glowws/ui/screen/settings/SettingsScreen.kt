package com.yaabelozerov.glowws.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.domain.model.SettingDomainModel

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    settings: Map<String, List<SettingDomainModel>>,
    onModify: (SettingsKeys, String) -> Unit
) {
    LazyColumn(modifier = modifier.padding(16.dp)) {
        items(settings.keys.toList()) { key ->
            Text(text = key, fontSize = 32.sp)
            Column {
                settings[key]!!.forEach { entry ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = entry.name)
                        Spacer(modifier = Modifier.weight(1f))
                        Column {
                            when (entry.value) {
                                is Boolean -> {
                                    val checked = remember { mutableStateOf(entry.value.toString() == "true") }
                                    Switch(checked = checked.value, onCheckedChange = {
                                        checked.value = !checked.value
                                        onModify(entry.key, checked.value.toString())
                                    })
                                }

                                is Double -> {
                                    Text(text = entry.value.toString())
                                    Button(onClick = { /*TODO*/ }) {
                                        Text(text = "Edit")
                                    }
                                }

                                is String -> {
                                    Text(text = entry.value.toString())
                                    Button(onClick = { /*TODO*/ }) {
                                        Text(text = "Edit")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}