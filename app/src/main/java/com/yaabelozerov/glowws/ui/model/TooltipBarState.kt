package com.yaabelozerov.glowws.ui.model

data class TooltipBarState(
    val show: Boolean = false,
    val messageResId: List<Int> = emptyList(),
    val onClick: () -> Unit = {}
)