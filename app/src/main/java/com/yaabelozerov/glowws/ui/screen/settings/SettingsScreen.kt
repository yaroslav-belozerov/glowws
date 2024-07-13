package com.yaabelozerov.glowws.ui.screen.settings

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.domain.model.BooleanSettingDomainModel
import com.yaabelozerov.glowws.domain.model.ChoiceSettingDomainModel
import com.yaabelozerov.glowws.domain.model.DoubleSettingDomainModel
import com.yaabelozerov.glowws.domain.model.MultipleChoiceSettingDomainModel
import com.yaabelozerov.glowws.domain.model.SettingDomainModel
import com.yaabelozerov.glowws.domain.model.StringSettingDomainModel
import dagger.hilt.android.qualifiers.ApplicationContext

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    settings: Map<Pair<String, ImageVector>, List<SettingDomainModel>>,
    onModify: (SettingsKeys, String) -> Unit
) {
    LazyColumn(
        modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillParentMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
        items(settings.keys.toList()) { key ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = key.second, contentDescription = "${key.first} icon")
                Text(text = key.first, fontSize = 24.sp)
            }
            Column {
                settings[key]!!.forEach { entry ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = entry.name)
                        Spacer(modifier = Modifier.weight(1f))
                        Column {
                            when (entry) {
                                is BooleanSettingDomainModel -> {
                                    val checked =
                                        remember { mutableStateOf(entry.value.toString() == "true") }
                                    Switch(checked = checked.value, onCheckedChange = {
                                        checked.value = !checked.value
                                        onModify(entry.key, checked.value.toString())
                                    }, modifier = Modifier.padding(0.dp, 0.dp, 8.dp, 0.dp))
                                }

                                is StringSettingDomainModel -> {
                                    Text(text = entry.value)
                                    Button(onClick = { /*TODO*/ }) {
                                        Text(text = "Edit")
                                    }
                                }

                                is DoubleSettingDomainModel -> {
                                    Text(text = entry.value.toString())
                                    Button(onClick = { /*TODO*/ }) {
                                        Text(text = "Edit")
                                    }
                                }

                                is ChoiceSettingDomainModel -> {
                                    val expanded = remember {
                                        mutableStateOf(false)
                                    }
                                    DropdownMenu(expanded = expanded.value,
                                        onDismissRequest = { expanded.value = false }) {
                                        entry.choices.forEach {
                                            Text(text = it, modifier = Modifier.clickable {
                                                onModify(
                                                    entry.key, it
                                                )
                                                expanded.value = false
                                            })
                                        }
                                    }
                                    if (!expanded.value) {
                                        Text(text = entry.value,
                                            modifier = Modifier.clickable { expanded.value = true })
                                    }
                                }

                                is MultipleChoiceSettingDomainModel -> {
                                    val expanded = remember {
                                        mutableStateOf(false)
                                    }
                                    DropdownMenu(expanded = expanded.value,
                                        onDismissRequest = { expanded.value = false }) {
                                        entry.choices.forEachIndexed { index, elem ->
                                            Row {
                                                if (entry.value[index]) Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null
                                                )
                                                Text(text = elem, modifier = Modifier.clickable {
                                                    onModify(entry.key,
                                                        entry.value.mapIndexed { i, it -> if (i == index) !it else it }
                                                            .joinToString(","))
                                                })
                                            }
                                        }
                                    }
                                    if (!expanded.value) {
                                        Text(text = if (entry.value.all { !it }) "None" else entry.choices.filterIndexed { index, s -> entry.value[index] }
                                            .joinToString(", "),
                                            modifier = Modifier.clickable { expanded.value = true })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "feedback icon")
                    Text(text = "Send feedback", fontSize = 24.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                val ctx = LocalContext.current
                val intentRu = remember {
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://forms.gle/RmvAob9n7Pi8UcGt8"))
                }
                val intentEn = remember {
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://forms.gle/R3TwjtoDqUS9PseTA"))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { ctx.startActivity(intentRu) }, Modifier.weight(1f)) {
                        Text(text = "RU \uD83C\uDDF7\uD83C\uDDFA")
                    }
                    OutlinedButton(onClick = { ctx.startActivity(intentEn) }, Modifier.weight(1f)) {
                        Text(text = "EN \uD83C\uDDFA\uD83C\uDDF8")
                    }
                }
            }
        }
    }
}