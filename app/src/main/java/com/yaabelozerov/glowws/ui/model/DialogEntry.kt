package com.yaabelozerov.glowws.ui.model

import androidx.compose.ui.graphics.vector.ImageVector

enum class ArchiveConfirmType {
    DELETE,
    UNARCHIVE
}

data class DialogEntry(
    val icon: ImageVector? = null,
    val name: String,
    val onClick: () -> Unit,
    val needsConfirmation: Boolean = false
)
