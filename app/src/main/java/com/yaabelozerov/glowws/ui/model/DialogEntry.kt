package com.yaabelozerov.glowws.ui.model

import androidx.compose.ui.graphics.vector.ImageVector

data class DialogEntry(
    val icon: ImageVector? = null,
    val name: String,
    val onClick: () -> Unit,
    val needsConfirmation: Boolean = false
)
