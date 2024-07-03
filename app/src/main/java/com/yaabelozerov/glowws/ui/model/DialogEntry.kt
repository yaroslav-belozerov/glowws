package com.yaabelozerov.glowws.ui.model

import androidx.compose.ui.graphics.vector.ImageVector

data class DialogEntry(
    val icon: ImageVector,
    val name: String,
    val onClick: () -> Unit
)
