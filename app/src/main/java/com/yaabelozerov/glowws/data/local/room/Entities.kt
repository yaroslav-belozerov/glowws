package com.yaabelozerov.glowws.data.local.room

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.Image
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yaabelozerov.glowws.R

@Entity
data class Idea(
    @PrimaryKey(autoGenerate = true) val ideaId: Long,
    val priority: Long,
    val isArchived: Boolean,
    val timestampCreated: Long,
    val timestampModified: Long,
    val mainPointId: Long
)

@Entity
data class Point(
    @PrimaryKey(autoGenerate = true) val pointId: Long,
    val ideaParentId: Long,
    var pointContent: String,
    val index: Long,
    var type: PointType,
    var isMain: Boolean,
)

enum class PointType(val resId: Int, val icon: ImageVector) {
  TEXT(R.string.i_point_type_text, Icons.Default.FormatColorText),
  IMAGE(R.string.i_point_type_image, Icons.Default.Image)
}

enum class ModelType(val resId: Int, vararg val variants: ModelVariant) {
  LOCAL(R.string.ai_local, ModelVariant.ONDEVICE, ModelVariant.DOWNLOADABLE),
}

enum class ModelVariant() {
  ONDEVICE,
  DOWNLOADABLE,
}

@Entity
data class Model(
    @PrimaryKey val initialName: String,
    val type: ModelVariant,
    val name: String?,
    val path: String?,
    val isChosen: Boolean = false
)
