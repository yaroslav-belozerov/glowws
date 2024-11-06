package com.yaabelozerov.glowws.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.PublishedWithChanges
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import com.yaabelozerov.glowws.R

enum class Prompt(val prompt: Int, val nameRes: Int, val icon: ImageVector) {
  FillIn(R.string.prompt_fill_in, R.string.ai_action_fill_in, Icons.AutoMirrored.Default.FormatListBulleted),
  Summarize(R.string.prompt_summarize, R.string.ai_action_summarize, Icons.Default.FilterAlt),
  Continue(R.string.prompt_continue, R.string.ai_action_continue, Icons.Default.SkipNext),
  Rephrase(R.string.prompt_rephrase, R.string.ai_action_rephrase, Icons.Default.PublishedWithChanges)
}