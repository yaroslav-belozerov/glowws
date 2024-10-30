package com.yaabelozerov.glowws.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.data.local.ai.InferenceManagerState
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.data.local.room.Model
import com.yaabelozerov.glowws.data.remote.FeedbackDTO
import com.yaabelozerov.glowws.domain.model.SettingDomainModel

@Composable
fun FeedbackScreen(
    modifier: Modifier = Modifier, onSendFeedback: (FeedbackDTO) -> Unit
) {
    var form by remember { mutableStateOf(FeedbackDTO("", 0, "")) }
    var displayError by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(form.header, {
            form = form.copy(header = it)
            displayError = false
        }, isError = displayError, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(
            stringResource(R.string.s_feedback_header)
        ) }, shape = MaterialTheme.shapes.medium)
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Slider(
                form.rating.toFloat(),
                onValueChange = { form = form.copy(rating = it.toLong()) },
                modifier = Modifier.fillMaxWidth(),
                valueRange = 0f..5f,
                steps = 4
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.s_feedback_rate), modifier = Modifier.weight(1f))
                Text(if (form.rating == 0L) stringResource(R.string.s_feedback_notrate) else form.rating.toString(), color = MaterialTheme.colorScheme.tertiary)
                if (form.rating != 0L) Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
            }
        }
        OutlinedTextField(
            value = form.desc, { form = form.copy(desc = it) }, modifier = Modifier.fillMaxWidth(), label = { Text(
                stringResource(R.string.s_feedback_desc)
            ) }, shape = MaterialTheme.shapes.medium
        )
        Button(modifier = Modifier.fillMaxWidth(), onClick = {
            if (form.header.isNotBlank()) onSendFeedback(form)
            else displayError = true
        }) { Text(stringResource(R.string.s_feedback_send)) }
        val density = LocalDensity.current
        AnimatedVisibility(visible = displayError,
            enter = slideInVertically {
                with(density) { -10.dp.roundToPx() }
            } + fadeIn(),
            exit = slideOutVertically { with(density) { -10.dp.roundToPx() } } + fadeOut()) {
            Text(text = stringResource(R.string.s_feedback_error), color = MaterialTheme.colorScheme.error)
        }
    }
}