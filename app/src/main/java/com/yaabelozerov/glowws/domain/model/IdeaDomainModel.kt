package com.yaabelozerov.glowws.domain.model

import com.yaabelozerov.glowws.data.local.room.PointType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.Boolean

data class JoinedTimestamp(val timestamp: Long, val string: String)

private const val pattern = "HH:mm dd.MM.yyyy"

fun LocalDateTime.toJoinedTimestamp(): JoinedTimestamp {
  val millis = this.toInstant(ZoneId.systemDefault().rules.getOffset(Instant.now())).toEpochMilli()
  val formatted = this.format(DateTimeFormatter.ofPattern(pattern))
  return JoinedTimestamp(millis, formatted)
}

data class IdeaDomainModel(
    val id: Long,
    val priority: Long,
    val created: JoinedTimestamp,
    val modified: JoinedTimestamp,
    val mainPoint: PointDomainModel
)

@Serializable
data class IdeaModel(
  val id: Long,
  val priority: Long,
  val isArchived: Boolean,
  @SerialName("timestampCreated") val created: String,
  @SerialName("timestampModified") val modified: String,
)

private val df = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")

fun IdeaModel.toDomainModel(): IdeaDomainModel {
  val cr = LocalDateTime.parse(created, df)
  val mod = LocalDateTime.parse(modified, df)
  return IdeaDomainModel(
    id = id,
    priority = priority,
    created = cr.toJoinedTimestamp(),
    modified = mod.toJoinedTimestamp(),
    mainPoint =
      PointDomainModel(-1, PointType.TEXT, "", false)
  )
}

fun List<IdeaModel>.archived() = filter { it.isArchived }
fun List<IdeaModel>.notArchived() = filter { !it.isArchived }
fun List<IdeaModel>.toDomain(): List<IdeaDomainModel> = map { it.toDomainModel() }