package com.yaabelozerov.glowws.data.local.room

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.Image
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
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
  TEXT(R.string.i_point_type_text, Icons.Default.FormatColorText), IMAGE(
    R.string.i_point_type_image,
    Icons.Default.Image
  )
}

sealed interface ModelType {
  data object OnDevice : ModelType
  data class Downloadable(val isDownloaded: Boolean) : ModelType
}

@Entity
data class Model(
  @PrimaryKey val name: String,
  val type: ModelType,
  val path: String?,
  val isChosen: Boolean = false
)

class ModelTypeConverters {
  @TypeConverter
  fun modelTypeFromValue(value: Long?): ModelType? = when (value) {
    0L -> ModelType.OnDevice
    1L -> ModelType.Downloadable(true)
    else -> ModelType.Downloadable(false)
  }

  @TypeConverter
  fun valueFromModelType(modelType: ModelType?): Long? {
    return when (modelType) {
      ModelType.OnDevice -> 0L
      is ModelType.Downloadable -> if (modelType.isDownloaded) 1L else 2L

      else -> null
    }
  }
}
