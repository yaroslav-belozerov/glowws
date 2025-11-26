package com.yaabelozerov.glowws.domain.model

import com.yaabelozerov.glowws.data.local.room.PointType
import kotlinx.serialization.Serializable

data class PointDomainModel(
  val id: Long,
  val type: PointType,
  val content: String,
  val isMain: Boolean
)

@Serializable
data class PointUpdateRequest(
  val pointContent: String,
  val isMain: Boolean
)

@Serializable
data class PointModel(
  val id: Long,
  val parentId: Long,
  val pointContent: String,
  val index: Long,
  val type: String,
  val isMain: Boolean
)

fun PointModel.toDomainModel() = PointDomainModel(
  id = id,
  type = when (type) {
    "TEXT" -> PointType.TEXT
    "IMAGE" -> PointType.IMAGE
    else -> PointType.TEXT
  },
  content = pointContent,
  isMain = isMain
)

fun List<PointModel>.toDomain() = map { it.toDomainModel() }