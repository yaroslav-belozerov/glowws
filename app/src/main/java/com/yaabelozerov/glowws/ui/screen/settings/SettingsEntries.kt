package com.yaabelozerov.glowws.ui.screen.settings

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.domain.model.BooleanSettingDomainModel
import com.yaabelozerov.glowws.domain.model.ChoiceSettingDomainModel
import com.yaabelozerov.glowws.domain.model.MultipleChoiceSettingDomainModel
import com.yaabelozerov.glowws.util.toReadableKey

@Composable
fun BooleanSettingsEntry(
    modifier: Modifier = Modifier,
    entry: BooleanSettingDomainModel,
    onModify: (SettingsKeys, String) -> Unit
) {
    var checked by remember { mutableStateOf(entry.value) }
    Row(
        modifier = modifier
            .clickable {
                checked = !checked
                onModify(entry.key, checked.toString())
            }
            .padding(16.dp, 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(entry.nameRes),
            fontSize = 24.sp,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = {
            checked = !checked
            onModify(entry.key, checked.toString())
        }, modifier = Modifier.padding(0.dp, 0.dp, 8.dp, 0.dp))
    }
}

@Composable
fun ChoiceSettingDomainEntry(
    modifier: Modifier = Modifier,
    entry: ChoiceSettingDomainModel,
    onModify: (SettingsKeys, String) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }
    Column(
        modifier = modifier
            .clickable { expanded = true }
            .padding(16.dp, 16.dp)
    ) {
        Text(text = stringResource(entry.nameRes), fontSize = 24.sp)
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            entry.choices.forEachIndexed { index, value ->
                val local = entry.localChoicesIds.getOrNull(index)
                Text(
                    text = if (local != null) stringResource(id = local) else value.toReadableKey(),
                    fontWeight = if (value == entry.value) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clickable {
                            onModify(
                                entry.key,
                                value
                            )
                            expanded = false
                        }
                        .padding(16.dp, 8.dp)
                )
            }
        }
        val local = entry.localChoicesIds.getOrNull(
            entry.choices.indexOf(entry.value)
        )
        Text(
            text = if (local != null) stringResource(id = local) else entry.value.toReadableKey(),
            modifier = Modifier.clickable { expanded = true }
        )
    }
}

@Composable
fun MultipleChoiceSettingsEntry(
    modifier: Modifier = Modifier,
    entry: MultipleChoiceSettingDomainModel,
    onModify: (SettingsKeys, String) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }
    Column(
        modifier = modifier
            .clickable { expanded = true }
            .padding(16.dp, 16.dp)
    ) {
        Text(text = stringResource(entry.nameRes), fontSize = 24.sp)
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            entry.choices.forEachIndexed { index, elem ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (entry.value[index]) {
                        Icon(
                            modifier = Modifier
                                .clickable {
                                    onModify(
                                        entry.key,
                                        entry.value
                                            .mapIndexed { i, value -> if (i == index) !value else value }
                                            .joinToString(",")
                                    )
                                }
                                .padding(16.dp, 0.dp, 0.dp, 0.dp),
                            imageVector = Icons.Default.Check,
                            contentDescription = null
                        )
                    }
                    val local = entry.localChoicesIds.getOrNull(index)
                    Text(
                        text = if (local != null) {
                            stringResource(
                                id = local
                            )
                        } else {
                            elem.toReadableKey()
                        },
                        modifier = Modifier
                            .clickable {
                                onModify(
                                    entry.key,
                                    entry.value
                                        .mapIndexed { i, value ->
                                            if (i == index) !value else value
                                        }
                                        .joinToString(
                                            ","
                                        )
                                )
                            }
                            .padding(16.dp, 8.dp)
                    )
                }
            }
        }
        Text(
            text = if (entry.value.all { !it }) {
                stringResource(id = R.string.placeholder_null)
            } else {
                entry.choices.mapIndexed { index, s ->
                    val local = entry.localChoicesIds.getOrNull(index)
                    if (local != null) stringResource(id = local) else s.toReadableKey()
                }.joinToString(", ")
            },
            modifier = Modifier.clickable { expanded = true }
        )
    }
}

@Composable
fun FeedbackSettingsEntry(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp, 0.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = Icons.Default.Info, contentDescription = "feedback icon")
            Text(text = stringResource(id = R.string.s_cat_feedback), fontSize = 32.sp)
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
