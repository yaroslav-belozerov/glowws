package com.yaabelozerov.glowws.domain.model

import com.yaabelozerov.glowws.data.local.room.PointType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.Boolean

data class JoinedTimestamp(val timestamp: Long, val string: String)

private const val pattern = "HH:mm dd.MM.yyyy"

fun Instant.toJoinedTimestamp(): JoinedTimestamp {
  val zoned = this.atZone(ZoneId.systemDefault())
  val formatted = zoned.format(DateTimeFormatter.ofPattern(pattern))
  return JoinedTimestamp(toEpochMilli(), formatted)
}

fun LocalDateTime.toJoinedTimestamp(): JoinedTimestamp {
  val zoned = this.atZone(ZoneId.systemDefault())
  val millis = zoned.toInstant().toEpochMilli()
  val formatted = zoned.format(DateTimeFormatter.ofPattern(pattern))
  return JoinedTimestamp(millis, formatted)
}

data class IdeaDomainModel(
  val id: Long,
  val priority: Long,
  val created: JoinedTimestamp,
  val modified: JoinedTimestamp,
  val mainPoint: PointDomainModel,
)

@Serializable
data class IdeaModel(
  val id: Long,
  val priority: Long,
  val isArchived: Boolean,
  @SerialName("timestampCreated") val created: String,
  @SerialName("timestampModified") val modified: String,
)

@Serializable
data class IdeaModelFull(
  val id: Long,
  val priority: Long,
  val isArchived: Boolean,
  @SerialName("timestampCreated") val created: String,
  @SerialName("timestampModified") val modified: String,
  val points: List<Map<String, String>>
)

private val df = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")

fun IdeaModelFull.toDomainModel(instanceUrl: String): IdeaDomainModel {
  val cr = Instant.parse(created)
  val mod = Instant.parse(modified)
  val pt = points.firstOrNull()
  val type = when (pt?.get("type")) {
    "IMAGE" -> PointType.IMAGE
    else -> PointType.TEXT
  }
  val content = when (type) {
    PointType.IMAGE -> pt?.get("pointContent")?.let { "$instanceUrl/$it" } ?: ""
    PointType.TEXT -> pt?.get("pointContent") ?: ""
  }
  return IdeaDomainModel(
    id = id,
    priority = priority,
    created = cr.toJoinedTimestamp(),
    modified = mod.toJoinedTimestamp(),
    mainPoint = PointDomainModel(-1, type, content, true, -1)
  )
}

fun List<IdeaModelFull>.archived() = filter { it.isArchived }
fun List<IdeaModelFull>.notArchived() = filter { !it.isArchived }
fun List<IdeaModelFull>.toDomain(instanceUrl: String): List<IdeaDomainModel> = map { it.toDomainModel(instanceUrl) }