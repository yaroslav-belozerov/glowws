package com.yaabelozerov.glowws.domain.model

import com.yaabelozerov.glowws.R

enum class Prompt(val prompt: Int, val nameRes: Int) {
  FillIn(R.string.prompt_fill_in, R.string.ai_action_fill_in),
  Summarize(R.string.prompt_summarize, R.string.ai_action_summarize),
  Rephrase(R.string.prompt_rephrase, R.string.ai_action_rephrase)
}