package com.yaabelozerov.glowws.domain.model

import com.yaabelozerov.glowws.data.local.room.PointType
import kotlinx.serialization.Serializable

data class PointDomainModel(
  val id: Long,
  val type: PointType,
  val content: String,
  val isMain: Boolean,
  val index: Long
)

@Serializable
data class PointUpdateRequest(
  val pointContent: String,
  val isMain: Boolean
)

@Serializable
data class PointMainUpdateRequest(
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

@Serializable
data class PointCreateRequest(
  val parentId: Long,
  val index: Long,
  val content: String,
  val type: String
)

fun PointModel.toDomainModel(instanceUrl: String) = PointDomainModel(
  id = id,
  type = when (type) {
    "TEXT" -> PointType.TEXT
    "IMAGE" -> PointType.IMAGE
    else -> PointType.TEXT
  },
  content = when (type) {
    "IMAGE" -> "$instanceUrl/$pointContent"
    else -> pointContent
  },
  isMain = isMain,
  index = index
)

fun List<PointModel>.toDomain(instanceUrl: String) = map { it.toDomainModel(instanceUrl) }