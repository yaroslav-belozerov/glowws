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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.domain.model.BooleanSettingDomainModel
import com.yaabelozerov.glowws.domain.model.ChoiceSettingDomainModel
import com.yaabelozerov.glowws.domain.model.DoubleSettingDomainModel
import com.yaabelozerov.glowws.domain.model.MultipleChoiceSettingDomainModel
import com.yaabelozerov.glowws.domain.model.SettingDomainModel
import com.yaabelozerov.glowws.domain.model.StringSettingDomainModel

fun String.toReadableKey() =
    replace("_", " ").lowercase().replaceFirstChar { char -> char.uppercase() }

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    settings: Map<Pair<Int, ImageVector>, List<SettingDomainModel>>,
    onModify: (SettingsKeys, String) -> Unit
) {
    LazyColumn(
        modifier = modifier
    ) {
        item {
            Text(
                text = stringResource(id = R.string.s_screen_name),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillParentMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
        items(settings.keys.toList(), key = { it }) { key ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp, 24.dp, 16.dp, 8.dp).animateItem()
            ) {
                Icon(imageVector = key.second, contentDescription = "${key.first} icon")
                Text(text = stringResource(id = key.first), fontSize = 32.sp)
            }
            settings[key]!!.forEach { entry ->
                when (entry) {
                    is BooleanSettingDomainModel -> {
                        val checked = remember { mutableStateOf(entry.value) }
                        Row(modifier = Modifier
                            .clickable {
                                checked.value = !checked.value
                                onModify(entry.key, checked.value.toString())
                            }
                            .padding(16.dp, 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(entry.nameRes),
                                fontSize = 24.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(checked = checked.value, onCheckedChange = {
                                checked.value = !checked.value
                                onModify(entry.key, checked.value.toString())
                            }, modifier = Modifier.padding(0.dp, 0.dp, 8.dp, 0.dp))
                        }
                    }

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

                    is ChoiceSettingDomainModel -> {
                        var expanded by remember {
                            mutableStateOf(false)
                        }
                        Column(modifier = Modifier
                            .clickable { expanded = true }
                            .fillParentMaxWidth()
                            .padding(16.dp, 16.dp)) {
                            Text(text = stringResource(entry.nameRes), fontSize = 24.sp)
                            DropdownMenu(expanded = expanded,
                                onDismissRequest = { expanded = false }) {
                                entry.choices.forEachIndexed { index, it ->
                                    val local = entry.localChoicesIds.getOrNull(index)
                                    Text(text = if (local != null) stringResource(id = local) else it.toReadableKey(),
                                        fontWeight = if (it == entry.value) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier
                                            .clickable {
                                                onModify(
                                                    entry.key, it
                                                )
                                                expanded = false
                                            }
                                            .padding(16.dp, 8.dp))
                                }
                            }
                            val local = entry.localChoicesIds.getOrNull(
                                entry.choices.indexOf(entry.value)
                            )
                            Text(text = if (local != null) stringResource(id = local) else entry.value.toReadableKey(),
                                modifier = Modifier.clickable { expanded = true })
                        }
                    }

                    is MultipleChoiceSettingDomainModel -> {
                        var expanded by remember {
                            mutableStateOf(false)
                        }
                        Column(modifier = Modifier
                            .clickable { expanded = true }
                            .fillParentMaxWidth()
                            .padding(16.dp, 16.dp)) {
                            Text(text = stringResource(entry.nameRes), fontSize = 24.sp)
                            DropdownMenu(expanded = expanded,
                                onDismissRequest = { expanded = false }) {
                                entry.choices.forEachIndexed { index, elem ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (entry.value[index]) Icon(modifier = Modifier
                                            .clickable {
                                                onModify(entry.key,
                                                    entry.value
                                                        .mapIndexed { i, it -> if (i == index) !it else it }
                                                        .joinToString(","))
                                            }
                                            .padding(16.dp, 0.dp, 0.dp, 0.dp),
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null)
                                        val local = entry.localChoicesIds.getOrNull(index)
                                        Text(text = if (local != null) stringResource(id = local) else elem.toReadableKey(),
                                            modifier = Modifier
                                                .clickable {
                                                    onModify(
                                                        entry.key,
                                                        entry.value
                                                            .mapIndexed { i, it ->
                                                                if (i == index) !it else it
                                                            }
                                                            .joinToString(
                                                                ","
                                                            )
                                                    )
                                                }
                                                .padding(16.dp, 8.dp))
                                    }
                                }
                            }
                            Text(text = if (entry.value.all { !it }) stringResource(id = R.string.placeholder_null) else entry.choices.mapIndexed { index, s ->
                                val local = entry.localChoicesIds.getOrNull(index)
                                if (local != null) stringResource(id = local) else s.toReadableKey()
                            }.joinToString(", "), modifier = Modifier.clickable { expanded = true })
                        }
                    }
                }
            }
        }
        item {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 0.dp)) {
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
    }
}
