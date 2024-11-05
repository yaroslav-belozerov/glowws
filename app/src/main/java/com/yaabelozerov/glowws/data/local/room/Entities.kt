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

enum class PointType(val title: String, val icon: ImageVector) {
  TEXT("Text", Icons.Default.FormatColorText),
  IMAGE("Image", Icons.Default.Image)
}

enum class ModelType(val resId: Int, vararg val variants: ModelVariant) {
  LOCAL(R.string.ai_local, ModelVariant.ONDEVICE),
  NETWORK(R.string.ai_network, ModelVariant.OPENROUTER, ModelVariant.GIGACHAT)
}

enum class ModelVariant(val needsToken: Boolean = false, val baseUrl: String = "") {
  ONDEVICE,
  OPENROUTER(true, "https://openrouter.ai/api/v1/"),
  GIGACHAT(true, "")
}

@Entity
data class Model(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val type: ModelVariant,
    val name: String?,
    val path: String?,
    val token: String? = null,
    val isChosen: Boolean = false
)
